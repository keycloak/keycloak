#!/usr/bin/env python3
"""Auto-label Keycloak issues using GitHub Copilot.

Two-step pipeline (sub-issues with a parent are skipped in both steps —
they are created by core contributors as subtasks and are usually not
bugs or enhancements):
  Step 1 — Type: issues without a GitHub issue type get one assigned.
           If the issue has a kind/* label matching a valid type, that is
           used directly. Otherwise, bug/enhancement is detected from the
           issue template body, or classified via Copilot. Issues typed
           from template/Copilot also get the status/triage label.
  Step 2 — Area: status/triage bug issues without an area/* label get an
           area assigned. Only areas with a team in teams.yml are eligible
           for assignment.

Area classification pipeline:
  1. Template: extract area from the bug template's '### Area' dropdown.
  2. Copilot: classify using area descriptions and issue content.

Usage:
    python3 auto_label.py --dry-run
    python3 auto_label.py --issue 12345 --dry-run
    python3 auto_label.py
"""

import argparse
import json
import os
import re
import subprocess
import sys

MAX_BODY_CHARS = 2000
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO = "keycloak/keycloak"


def detect_kind_from_body(body):
    """Detect issue kind from template markers in the body."""
    if not body:
        return None
    if "### Describe the bug" in body:
        return "bug"
    if "### Value Proposition" in body:
        return "enhancement"
    return None


def extract_area_from_body(body, valid_areas):
    """Extract area from the bug template's '### Area' dropdown."""
    if not body:
        return None
    match = re.search(r"### Area\s*\n\s*(.+)", body)
    if not match:
        return None
    area_text = match.group(1).strip()
    if not area_text or area_text == "_No response_":
        return None
    candidate = f"area/{area_text}"
    if candidate in valid_areas:
        return candidate
    return None


def load_assignable_areas():
    """Load area labels that have a team assigned in teams.yml.

    Areas without a team are excluded because assigning them would leave
    the issue without anyone to triage it.
    """
    teams_path = os.path.join(SCRIPT_DIR, "..", "teams.yml")
    with open(teams_path) as f:
        content = f.read()

    areas = set()
    current_team = None
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.endswith(":") and not stripped.startswith("-"):
            current_team = stripped[:-1]
        elif stripped.startswith("- area/") and current_team:
            if current_team.startswith("team/"):
                areas.add(stripped[2:])
    return areas



def fetch_issue_types():
    """Fetch all issue types defined in the repository."""
    result = subprocess.run(
        ["gh", "api", f"repos/{REPO}/issue-types", "--jq", ".[].name"],
        capture_output=True, text=True, check=True,
    )
    return [t.strip() for t in result.stdout.strip().split("\n") if t.strip()]


def fetch_untyped_issues(issue_types):
    """Fetch open issues without an issue type (step 1 candidates).

    Returns each issue with a 'kind_label' field if a kind/* label exists,
    so it can be used directly as the type without classification.
    """
    exclusions = " ".join(f"-type:{t}" for t in issue_types)
    result = subprocess.run(
        ["gh", "issue", "list", "--repo", REPO, "--state", "open",
         "--search", f"no:parent-issue {exclusions}",
         "--json", "number,title,labels,body", "--limit", "100"],
        capture_output=True, text=True, check=True,
    )
    issues = json.loads(result.stdout)
    type_set = {t.lower() for t in issue_types}
    for issue in issues:
        labels = [l["name"] for l in issue.get("labels", [])]
        kind = None
        for l in labels:
            if l.startswith("kind/"):
                k = l.removeprefix("kind/")
                if k in type_set:
                    kind = k
                    break
        issue["kind_label"] = kind
        issue["_body"] = issue.pop("body", "") or ""
    return issues


def fetch_triage_issues_needing_area(all_areas):
    """Fetch open status/triage bug issues without an area label."""
    exclusions = " ".join(f"-label:{a}" for a in sorted(all_areas))
    result = subprocess.run(
        ["gh", "issue", "list", "--repo", REPO, "--state", "open",
         "--label", "status/triage",
         "--search", f"type:bug no:parent-issue {exclusions}",
         "--json", "number,title,labels,body", "--limit", "100"],
        capture_output=True, text=True, check=True,
    )
    issues = json.loads(result.stdout)
    filtered = []
    for issue in issues:
        labels = [l["name"] for l in issue.get("labels", [])]
        if any(l.startswith("area/") for l in labels):
            continue
        issue["_body"] = issue.pop("body", "") or ""
        filtered.append(issue)
    return filtered


def fetch_issue_body(issue_number):
    """Fetch issue body via gh CLI."""
    result = subprocess.run(
        ["gh", "issue", "view", str(issue_number), "--repo", REPO,
         "--json", "body"],
        capture_output=True, text=True, check=True,
    )
    data = json.loads(result.stdout)
    return data.get("body", "") or ""


def fetch_area_labels():
    """Fetch area/* labels from GitHub and return valid areas + descriptions."""
    result = subprocess.run(
        ["gh", "api", f"repos/{REPO}/labels", "--paginate",
         "--jq", '.[] | select(.name | startswith("area/")) | {name, description}'],
        capture_output=True, text=True, check=True,
    )
    valid_areas = set()
    descriptions = {}
    for line in result.stdout.strip().split("\n"):
        if not line.strip():
            continue
        label = json.loads(line)
        name = label["name"]
        valid_areas.add(name)
        desc = label.get("description") or ""
        if desc:
            descriptions[name.removeprefix("area/")] = desc
    return valid_areas, descriptions


def build_prompt(title, body, valid_areas, need_kind, need_area,
                 area_descriptions=None):
    """Build the classification prompt with issue content."""
    body_truncated = body[:MAX_BODY_CHARS] if body else "(no body)"

    prompt = (
        "You classify GitHub issues for Keycloak, an open-source "
        "identity and access management server.\n\n"
        "Respond with a JSON object only, no other text.\n"
    )

    if need_kind:
        prompt += (
            '\n- "kind": either "bug" (something broken, error, regression, '
            'incorrect behavior) or "enhancement" (improvement to an existing '
            "feature, new capability, better UX).\n"
            "  If the issue reports something that used to work or produces an "
            "error/exception, it is a bug.\n"
            "  If it asks for something new or better, it is an enhancement.\n"
        )

    if need_area:
        prompt += (
            '\n- "area": the single most relevant component area from the '
            "list below. Use only values from this list.\n\n"
        )
        if area_descriptions:
            for area, desc in sorted(area_descriptions.items()):
                prompt += f"  {area}: {desc}\n"
        else:
            prompt += "  " + ", ".join(sorted(
                a.removeprefix("area/") for a in valid_areas
            )) + "\n"

    prompt += (
        f"\nClassify this issue:\n\n"
        f"Title: {title}\n\n"
        f"Body:\n{body_truncated}"
    )

    return prompt


COPILOT_SAFETY_FLAGS = [
    "--available-tools=",
    "--disable-builtin-mcps",
    "--no-custom-instructions",
    "--no-remote",
    "--no-remote-export",
]


def call_copilot(prompt):
    """Call Copilot CLI as a pure LLM (no tools, no MCP, no remote).

    Passes the prompt via stdin to avoid argv length limits and process
    visibility. Uses the 'copilot' CLI directly (installed via npm
    @github/copilot). In GitHub Actions, set COPILOT_GITHUB_TOKEN to
    ${{ github.token }} with copilot-requests: write permission.
    Falls back to 'gh copilot' for local use.
    """
    for cmd in [
        ["copilot", "-s", "--no-ask-user"] + COPILOT_SAFETY_FLAGS,
        ["gh", "copilot", "-s"] + COPILOT_SAFETY_FLAGS,
    ]:
        try:
            result = subprocess.run(
                cmd, input=prompt,
                capture_output=True, text=True, timeout=60,
            )
            if result.returncode == 0:
                return result.stdout.strip()
        except FileNotFoundError:
            continue

    print("  Copilot CLI not available", file=sys.stderr)
    return None


def parse_llm_response(response_text):
    """Extract JSON from LLM response."""
    if not response_text:
        return None

    try:
        return json.loads(response_text.strip())
    except json.JSONDecodeError:
        pass

    fence_match = re.search(
        r"```(?:json)?\s*(\{.*?\})\s*```", response_text, re.DOTALL
    )
    if fence_match:
        try:
            return json.loads(fence_match.group(1))
        except json.JSONDecodeError:
            pass

    json_match = re.search(r"\{[^{}]*\}", response_text)
    if json_match:
        try:
            return json.loads(json_match.group(0))
        except json.JSONDecodeError:
            pass

    return None


def normalize_area(area_str, valid_areas):
    """Normalize an area string to its full 'area/...' form, or None if invalid."""
    if not area_str:
        return None
    if area_str in valid_areas:
        return area_str
    prefixed = f"area/{area_str}"
    if prefixed in valid_areas:
        return prefixed
    return None


def classify_issue(title, body, valid_areas, need_kind, need_area,
                   area_descriptions=None):
    """Classify a single issue using Copilot."""
    prompt = build_prompt(
        title, body, valid_areas, need_kind, need_area, area_descriptions,
    )
    print(f"  Prompt: {len(prompt)} chars")

    response = call_copilot(prompt)
    parsed = parse_llm_response(response)

    kind_result = None
    area_result = None

    if parsed:
        print(f"  Copilot response: {json.dumps(parsed)}")

        if need_kind:
            k = parsed.get("kind", "")
            if k in ("bug", "enhancement"):
                kind_result = k

        if need_area:
            a = parsed.get("area", "")
            a_normalized = normalize_area(a, valid_areas)
            if a_normalized:
                area_result = a_normalized
            else:
                print(f"  Copilot area '{a}' not valid, ignoring")
    else:
        print(f"  Copilot failed to return valid JSON")

    return {"area": area_result, "kind": kind_result}


def set_issue_type(issue_number, kind):
    """Set the GitHub issue type (bug/enhancement)."""
    result = subprocess.run(
        ["gh", "issue", "edit", str(issue_number), "--repo", REPO,
         "--type", kind],
        capture_output=True, text=True,
    )
    if result.returncode == 0:
        print(f"  Set issue type: {kind}")
    else:
        print(f"  WARN: failed to set type '{kind}' — {result.stderr.strip()}",
              file=sys.stderr)


def add_labels(issue_number, labels):
    """Add labels to an issue."""
    label_str = ",".join(labels)
    result = subprocess.run(
        ["gh", "issue", "edit", str(issue_number), "--repo", REPO,
         "--add-label", label_str],
        capture_output=True, text=True,
    )
    if result.returncode == 0:
        print(f"  Applied labels: {label_str}")
    else:
        print(f"  WARN: failed to apply labels — {result.stderr.strip()}",
              file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(
        description="Auto-label Keycloak issues using Copilot")
    parser.add_argument("--issue", type=str, help="Single issue number")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--no-copilot", action="store_true",
                        help="Skip Copilot classification")
    args = parser.parse_args()

    issue_types = fetch_issue_types()
    print(f"Issue types: {', '.join(issue_types)}")

    team_areas = load_assignable_areas()
    all_areas, area_descriptions = fetch_area_labels()
    ghost_areas = team_areas - all_areas
    if ghost_areas:
        print(f"WARNING: {len(ghost_areas)} area(s) in teams.yml but not on GitHub: "
              + ", ".join(sorted(ghost_areas)))
    valid_areas = team_areas & all_areas
    print(f"Assignable areas: {len(valid_areas)} (areas with a team in teams.yml)")

    unassigned = all_areas - valid_areas
    if unassigned:
        print(f"Excluded {len(unassigned)} area(s) without a team: "
              + ", ".join(sorted(unassigned)))
    area_descriptions = {a: d for a, d in area_descriptions.items()
                         if f"area/{a}" in valid_areas}
    print(f"Area descriptions: {len(area_descriptions)} with descriptions")

    if args.issue:
        result = subprocess.run(
            ["gh", "issue", "view", args.issue, "--repo", REPO,
             "--json", "title,labels,issueType"],
            capture_output=True, text=True, check=True,
        )
        data = json.loads(result.stdout)
        title = data["title"]
        labels = [l["name"] for l in data.get("labels", [])]
        issue_type = (data.get("issueType") or {}).get("name", "").lower()
        has_area = any(l.startswith("area/") for l in labels)
        type_set = {t.lower() for t in issue_types}
        kind_label = None
        for l in labels:
            if l.startswith("kind/"):
                k = l.removeprefix("kind/")
                if k in type_set:
                    kind_label = k
                    break
        body = fetch_issue_body(int(args.issue))
        single = {
            "number": int(args.issue), "title": title,
            "issue_type": issue_type, "has_area": has_area,
            "kind_label": kind_label, "_body": body,
        }

        if args.dry_run:
            step1_issues = [single]
            step2_issues = [single]
        else:
            if not issue_type:
                step1_issues = [single]
            else:
                step1_issues = []

            is_or_will_be_bug = issue_type == "bug" or (not issue_type and kind_label == "bug")
            if is_or_will_be_bug and not has_area:
                step2_issues = [single]
            else:
                step2_issues = []
    else:
        step1_issues = fetch_untyped_issues(issue_types)
        step2_issues = None

    # Step 1: assign issue type (bug/enhancement) and status/triage
    print(f"\n=== Step 1: {len(step1_issues)} issue(s) without a type ===")

    for issue in step1_issues:
        number = issue["number"]
        title = issue["title"]

        print(f"\nhttps://github.com/{REPO}/issues/{number}")

        body = issue.get("_body") or ""
        kind = issue.get("kind_label")
        if kind:
            print(f"  kind/{kind} label present, using as type")
        else:
            kind = detect_kind_from_body(body)
            if kind:
                print(f"  Detected {kind} from issue template")
            else:
                if not args.no_copilot:
                    result = classify_issue(
                        title, body, valid_areas,
                        need_kind=True, need_area=False,
                    )
                    kind = result.get("kind")

        if not kind:
            print(f"  -> SKIP (no kind)")
            continue

        from_label = issue.get("kind_label") is not None
        if args.dry_run:
            triage_note = "" if from_label else ", +status/triage"
            print(f"  -> DRY RUN: type={kind}{triage_note}")
        else:
            set_issue_type(number, kind)
            if not from_label:
                add_labels(number, ["status/triage"])

    # Step 2: assign area to bug issues with status/triage
    if step2_issues is None:
        step2_issues = fetch_triage_issues_needing_area(all_areas)

    print(f"\n=== Step 2: {len(step2_issues)} issue(s) needing an area ===")

    for issue in step2_issues:
        number = issue["number"]
        title = issue["title"]
        body = issue.get("_body") or fetch_issue_body(number)

        print(f"\nhttps://github.com/{REPO}/issues/{number}")

        area = extract_area_from_body(body, valid_areas)
        if area:
            print(f"  Area from issue template: {area}")
        else:
            if not args.no_copilot:
                result = classify_issue(
                    title, body, valid_areas,
                    need_kind=False, need_area=True,
                    area_descriptions=area_descriptions,
                )
                area = result.get("area")

        if not area:
            print(f"  -> SKIP (no area)")
            continue

        if args.dry_run:
            print(f"  -> DRY RUN: area={area}")
        else:
            add_labels(number, [area])


if __name__ == "__main__":
    main()
