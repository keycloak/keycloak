#!/bin/sh
case "$(uname)" in
    CYGWIN*)
        CFILE="$(cygpath "$0")"
        RESOLVED_NAME="$(readlink -f "$CFILE")"
        ;;
    Darwin*)
        RESOLVED_NAME="$(readlink "$0")"
        ;;
    OpenBSD)
        RESOLVED_NAME="$(readlink -f "$0")"
        JAVA_HOME="$(/usr/local/bin/javaPathHelper -h keycloak)"
        ;;
    FreeBSD | Linux)
        RESOLVED_NAME="$(readlink -f "$0")"
        ;;
esac

RESOLVED_NAME="${RESOLVED_NAME:-"$0"}"

DIRNAME="$(dirname "$RESOLVED_NAME")"

if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

# Shell completion script generation (no JVM needed)
if [ "$1" = "--v2" ] && [ "$2" = "completion" ]; then
    COMP_SHELL="${3:-$(basename "$SHELL")}"
    case "$COMP_SHELL" in
        bash)
            cat << 'BASH_COMPLETION'
__kcadm_v2_complete() {
    local -r v2_flag="--v2"
    local -r cur="${COMP_WORDS[COMP_CWORD]}"
    local args=()
    local saw_v2=false

    for ((i=1; i < COMP_CWORD; i++)); do
        case "${COMP_WORDS[$i]}" in
            "$v2_flag") saw_v2=true ;;
            *) args+=("${COMP_WORDS[$i]}") ;;
        esac
    done

    if [[ "$saw_v2" != true ]]; then
        [[ "$cur" == "$v2_flag" ]] && COMPREPLY=("$v2_flag")
        return
    fi

    args+=("${cur}")

    local IFS=$'\n'
    local completions
    completions=$(kcadm.sh "$v2_flag" __complete "${args[@]}" 2>/dev/null)
    COMPREPLY=($(compgen -W "${completions}" -- "${cur}"))
}

complete -o default -F __kcadm_v2_complete kcadm.sh
BASH_COMPLETION
            ;;
        zsh)
            cat << 'ZSH_COMPLETION'
#compdef kcadm.sh

_kcadm_v2() {
    local -a args
    local saw_v2=false

    for ((i=2; i < CURRENT; i++)); do
        case "${words[$i]}" in
            --v2) saw_v2=true ;;
            *) args+=("${words[$i]}") ;;
        esac
    done

    if [[ "$saw_v2" != true ]]; then
        return
    fi

    args+=("${words[CURRENT]}")

    local -a completions
    completions=($(kcadm.sh --v2 __complete "${args[@]}" 2>/dev/null))
    compadd -a completions
}

compdef _kcadm_v2 kcadm.sh
ZSH_COMPLETION
            ;;
        fish)
            cat << 'FISH_COMPLETION'
function __kcadm_v2_needs_completion
    set -l cmd (commandline -opc)
    contains -- --v2 $cmd
end

function __kcadm_v2_completions
    set -l tokens (commandline -opc)
    set -l args
    for i in (seq 2 (count $tokens))
        switch $tokens[$i]
            case --v2
            case '*'
                set -a args $tokens[$i]
        end
    end
    set -l cur (commandline -ct)
    set -a args $cur
    kcadm.sh --v2 __complete $args 2>/dev/null
end

complete -c kcadm.sh -f -n __kcadm_v2_needs_completion -a '(__kcadm_v2_completions)'
FISH_COMPLETION
            ;;
        *)
            echo "Unsupported shell: $COMP_SHELL. Usage: kcadm.sh --v2 completion [bash|zsh|fish]" >&2
            exit 1
            ;;
    esac
    exit 0
fi

exec "$JAVA" $KC_OPTS -cp $DIRNAME/client/keycloak-admin-cli-${project.version}.jar --add-opens=java.base/java.security=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Dkc.lib.dir=$DIRNAME/client/lib org.keycloak.client.admin.cli.KcAdmMain "$@"
