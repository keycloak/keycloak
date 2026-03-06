#!/usr/bin/env python3
"""Migrate META-INF/services files to @AutoService annotations."""

import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent

# Exclusions: skip these service files entirely
EXCLUDED_SERVICE_FILES = {
    "org.freedesktop.dbus.spi.transport.ITransportProvider",
    "org.jboss.arquillian.core.spi.LoadableExtension",
}

CLASS_DECL_RE = re.compile(
    r'^(public |protected |private |abstract |final |sealed |non-sealed |static )*'
    r'(class |enum |@?interface )\s*\w+'
)

AUTOSERVICE_IMPORT = "import com.google.auto.service.AutoService;"

# Stats
stats = {
    "service_files_processed": 0,
    "service_files_skipped": 0,
    "service_files_deleted": 0,
    "service_files_trimmed": 0,
    "service_files_unchanged": 0,
    "java_files_annotated": 0,
    "dirs_cleaned": 0,
}


def find_service_files():
    """Find all META-INF/services files, excluding target/ and src/test/resources/ directories."""
    result = []
    for root, dirs, files in os.walk(REPO_ROOT):
        # Skip target directories
        dirs[:] = [d for d in dirs if d != "target"]
        root_path = Path(root)
        if root_path.name == "services" and "META-INF" in str(root_path):
            # Verify it's under META-INF/services/
            parts = root_path.parts
            if len(parts) >= 2 and parts[-2] == "META-INF" and parts[-1] == "services":
                # Skip test resources — @AutoService may not reliably generate
                # service files for test sources across all build environments
                path_str = str(root_path)
                if "/src/test/resources/" in path_str:
                    continue
                for f in files:
                    result.append(root_path / f)
    return sorted(result)


def get_module_root(service_file_path):
    """Extract the module root from a service file path.

    e.g. .../quarkus/runtime/src/main/resources/META-INF/services/foo
    -> .../quarkus/runtime/
    """
    parts = service_file_path.parts
    for i, part in enumerate(parts):
        if part == "src" and i + 2 < len(parts) and parts[i + 2] == "resources":
            return Path(*parts[:i])
    return None


def get_source_root(service_file_path):
    """Determine the source root (src/main/java or src/test/java) based on the service file location."""
    path_str = str(service_file_path)
    if "/src/test/" in path_str:
        return "src/test/java"
    return "src/main/java"


def parse_service_file(path):
    """Parse a service file, returning list of implementation FQCNs."""
    impls = []
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or line.startswith("!"):
                continue
            impls.append(line)
    return impls


def fqcn_to_path(fqcn):
    """Convert a FQCN to a relative path."""
    return fqcn.replace(".", "/") + ".java"


def get_package(fqcn):
    """Get the package from a FQCN."""
    idx = fqcn.rfind(".")
    return fqcn[:idx] if idx >= 0 else ""


def get_simple_name(fqcn):
    """Get the simple class name from a FQCN."""
    idx = fqcn.rfind(".")
    return fqcn[idx + 1:] if idx >= 0 else fqcn


def add_autoservice_annotation(java_file, service_fqcn):
    """Add @AutoService annotation to a Java file.

    Returns True if the file was modified.
    """
    with open(java_file) as f:
        content = f.read()
    lines = content.split("\n")

    service_simple = get_simple_name(service_fqcn)

    # Check if already has @AutoService for this exact service
    existing_autoservice = None
    existing_autoservice_line = -1
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("@AutoService("):
            existing_autoservice = stripped
            existing_autoservice_line = i
            break

    if existing_autoservice:
        # Check if it already references this service
        if service_simple + ".class" in existing_autoservice:
            return False  # Already annotated

        # Merge: convert single to multi-value or add to existing multi-value
        if existing_autoservice.startswith("@AutoService({"):
            # Multi-value form: @AutoService({A.class, B.class})
            # Insert before the closing })
            old_line = lines[existing_autoservice_line]
            insert_pos = old_line.rindex("})")
            new_line = old_line[:insert_pos] + ", " + service_simple + ".class})"
            lines[existing_autoservice_line] = new_line
        else:
            # Single value form: @AutoService(A.class)
            # Extract existing class ref
            m = re.search(r'@AutoService\((.+?\.class)\)', existing_autoservice)
            if m:
                existing_ref = m.group(1)
                indent = lines[existing_autoservice_line][:len(lines[existing_autoservice_line]) - len(lines[existing_autoservice_line].lstrip())]
                lines[existing_autoservice_line] = f"{indent}@AutoService({{{existing_ref}, {service_simple}.class}})"

        # Add import for the service interface if needed
        lines = ensure_import(lines, service_fqcn, java_file)

        with open(java_file, "w") as f:
            f.write("\n".join(lines))
        return True

    # No existing @AutoService - add it fresh
    # Find the class declaration line
    class_line_idx = -1
    for i, line in enumerate(lines):
        if CLASS_DECL_RE.match(line.lstrip()):
            # Make sure it's a top-level declaration (not indented significantly or inside a comment)
            stripped = line.lstrip()
            indent_level = len(line) - len(stripped)
            if indent_level <= 0:
                class_line_idx = i
                break

    if class_line_idx == -1:
        print(f"  WARNING: Could not find class declaration in {java_file}")
        return False

    # Add @AutoService annotation before class declaration
    indent = lines[class_line_idx][:len(lines[class_line_idx]) - len(lines[class_line_idx].lstrip())]
    annotation = f"{indent}@AutoService({service_simple}.class)"
    lines.insert(class_line_idx, annotation)

    # Add imports
    lines = ensure_import(lines, "com.google.auto.service.AutoService", None)
    lines = ensure_import(lines, service_fqcn, java_file)

    with open(java_file, "w") as f:
        f.write("\n".join(lines))
    return True


def ensure_import(lines, fqcn, java_file):
    """Ensure an import statement exists for the given FQCN.

    Returns the (possibly modified) lines list.
    """
    import_stmt = f"import {fqcn};"

    # Check if already imported
    for line in lines:
        if line.strip() == import_stmt:
            return lines

    # Check if same package (no import needed)
    if java_file:
        for line in lines:
            stripped = line.strip()
            if stripped.startswith("package "):
                pkg = stripped[len("package "):-1].strip()  # remove trailing ;
                if pkg == get_package(fqcn):
                    return lines
                break

    # Find the right place to insert the import (after package, among other imports)
    # Find last import line or package line
    last_import_idx = -1
    package_idx = -1
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("package "):
            package_idx = i
        if stripped.startswith("import "):
            last_import_idx = i

    if last_import_idx >= 0:
        # Insert after the last import
        lines.insert(last_import_idx + 1, import_stmt)
    elif package_idx >= 0:
        # Insert after package statement (with blank line)
        lines.insert(package_idx + 1, "")
        lines.insert(package_idx + 2, import_stmt)
    else:
        # No package or import - insert at beginning
        lines.insert(0, import_stmt)

    return lines


def cleanup_empty_dirs(service_file_path):
    """Clean up empty META-INF/services/, META-INF/, resources/ directories."""
    services_dir = service_file_path.parent
    for d in [services_dir]:
        if d.exists() and not any(d.iterdir()):
            d.rmdir()
            stats["dirs_cleaned"] += 1
            # Try META-INF
            meta_inf = d.parent
            if meta_inf.exists() and not any(meta_inf.iterdir()):
                meta_inf.rmdir()
                stats["dirs_cleaned"] += 1
                # Try resources
                resources = meta_inf.parent
                if resources.exists() and not any(resources.iterdir()):
                    resources.rmdir()
                    stats["dirs_cleaned"] += 1


def process_service_file(service_file):
    """Process a single service file."""
    service_name = service_file.name

    # Check exclusions
    if service_name in EXCLUDED_SERVICE_FILES:
        stats["service_files_skipped"] += 1
        print(f"  SKIP (excluded): {service_file.relative_to(REPO_ROOT)}")
        return

    stats["service_files_processed"] += 1

    module_root = get_module_root(service_file)
    if module_root is None:
        print(f"  WARNING: Cannot determine module root for {service_file}")
        stats["service_files_unchanged"] += 1
        return

    source_root = get_source_root(service_file)
    impls = parse_service_file(service_file)

    if not impls:
        stats["service_files_unchanged"] += 1
        print(f"  UNCHANGED (empty): {service_file.relative_to(REPO_ROOT)}")
        return

    local_impls = []
    remote_impls = []

    for impl_fqcn in impls:
        java_path = module_root / source_root / fqcn_to_path(impl_fqcn)
        if java_path.exists():
            local_impls.append((impl_fqcn, java_path))
        else:
            remote_impls.append(impl_fqcn)

    # Annotate local impls
    for impl_fqcn, java_path in local_impls:
        if add_autoservice_annotation(java_path, service_name):
            stats["java_files_annotated"] += 1
            print(f"  ANNOTATED: {java_path.relative_to(REPO_ROOT)}")

    # Handle the service file
    if not local_impls:
        # All remote - keep unchanged
        stats["service_files_unchanged"] += 1
        print(f"  UNCHANGED (all remote): {service_file.relative_to(REPO_ROOT)}")
    elif not remote_impls:
        # All local - delete
        os.remove(service_file)
        stats["service_files_deleted"] += 1
        print(f"  DELETED: {service_file.relative_to(REPO_ROOT)}")
        cleanup_empty_dirs(service_file)
    else:
        # Mixed - trim to remote only
        with open(service_file, "w") as f:
            for impl in remote_impls:
                f.write(impl + "\n")
        stats["service_files_trimmed"] += 1
        print(f"  TRIMMED: {service_file.relative_to(REPO_ROOT)} (kept {len(remote_impls)} remote entries)")


def main():
    print("Finding service files...")
    service_files = find_service_files()
    print(f"Found {len(service_files)} service files\n")

    for sf in service_files:
        rel = sf.relative_to(REPO_ROOT)
        print(f"Processing: {rel}")
        process_service_file(sf)

    print("\n=== Summary ===")
    print(f"Service files processed: {stats['service_files_processed']}")
    print(f"Service files skipped:   {stats['service_files_skipped']}")
    print(f"Service files deleted:   {stats['service_files_deleted']}")
    print(f"Service files trimmed:   {stats['service_files_trimmed']}")
    print(f"Service files unchanged: {stats['service_files_unchanged']}")
    print(f"Java files annotated:    {stats['java_files_annotated']}")
    print(f"Empty dirs cleaned:      {stats['dirs_cleaned']}")


if __name__ == "__main__":
    main()
