# Establish global variables to the docs and script dirs
CURRENT_DIR="$( pwd -P)"
SCRIPT_SRC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"
DOCS_SRC="$( dirname $SCRIPT_SRC )"
BUILD_RESULTS="Build Results:"
BUILD_MESSAGE=$BUILD_RESULTS
BLACK='\033[0;30m'
RED='\033[0;31m'
NO_COLOR="\033[0m"

usage(){
  cat <<EOM
USAGE: $0 [OPTION]... <guide>

DESCRIPTION: Build all of the guides (default) or a single guide.

Run this script from either the root of your cloned repo or from the 'scripts'
directory.  Example:
  cd eap-documentation/scripts
  $0

OPTIONS:
  -h       Print help.

EXAMPLES:
  Build all guides:
   $0

  Build a specific guide(s) from $DOCS_SRC:
    $0 authorization_services
    $0 securing_apps
    $0 authorization_services securing_apps

EOM
# Now list the valid book values
listvalidbooks
}

listvalidbooks(){
  echo ""
  echo "  Valid book argument values are:"
  cd $DOCS_SRC
  subdirs=`find . -maxdepth 1 -type d ! -iname ".*" ! -iname "topics" ! -iname "images" ! -iname "scripts" | sort`
  for subdir in $subdirs
  do
    echo "   ${subdir##*/}"
  done
  echo ""
  # Return to where we started as a courtesy.
  cd $CURRENT_DIR
}

OPTIND=1
while getopts "h" c
 do
     case "$c" in
       h)         usage
                  exit 1;;
       \?)        echo "Unknown option: -$OPTARG." >&2
                  usage
                  exit 1;;
     esac
 done
shift $(($OPTIND - 1))

# Use $DOCS_SRC so we don't have to worry about relative paths.
cd $DOCS_SRC

# Set the list of docs to build to whatever the user passed in (if anyting)
subdirs=$@
if [ $# -gt 0 ]; then
  echo "=== Bulding $@ ==="
else
  echo "=== Building all the guides ==="
  # Recurse through the guide directories and build them.
  subdirs=`find . -maxdepth 1 -type d ! -iname ".*" ! -iname "topics" ! -iname "images" ! -iname "scripts" | sort`
fi
echo $PWD
for subdir in $subdirs
do
  echo "Building $DOCS_SRC/${subdir##*/}"
  # Navigate to the dirctory and build it
  if ! [ -e $DOCS_SRC/${subdir##*/} ]; then
    BUILD_MESSAGE="$BUILD_MESSAGE\nERROR: $DOCS_SRC/${subdir##*/} does not exist."
    # This is a book argument error so we should list the valid arguments.
    LIST_BOOKS="true"
    continue
  fi
  cd $DOCS_SRC/${subdir##*/}
  GUIDE_NAME=${PWD##*/}
  python gitlab-conversion.py
  ./buildGuide.sh

  if [ "$?" = "1" ]; then
    BUILD_ERROR="ERROR: Build of $GUIDE_NAME failed. See the log above for details."
    BUILD_MESSAGE="$BUILD_MESSAGE\n$BUILD_ERROR"
  fi

  # Return to the parent directory
  cd $SCRIPT_SRC
done

# Return to where we started as a courtesy.
cd $CURRENT_DIR

# Report any errors
echo ""
if [ "$BUILD_MESSAGE" == "$BUILD_RESULTS" ]; then
  echo "Build was successful!"
else
  echo -e "${RED}$BUILD_MESSAGE${NO_COLOR}"
  if [ "$LIST_BOOKS" ]; then
    listvalidbooks
  else
    # This is a build error.
    echo -e "${RED}Please fix all issues before requesting a merge!${NO_COLOR}"
  fi
fi
exit;
