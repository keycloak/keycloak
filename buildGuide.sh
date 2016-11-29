# Build the guide

# Find the directory name and full path
CURRENT_GUIDE=${PWD##*/}
CURRENT_DIRECTORY=$(pwd)

usage(){
  cat <<EOM
USAGE: $0 [OPTION]

DESCRIPTION: Build the documentation in this directory.

OPTIONS:
  -h       Print help.

EOM
}

while getopts "ht:" c
 do
     case "$c" in
       h)         usage
                  exit 1;;
       \?)        echo "Unknown option: -$OPTARG." >&2
                  usage
                  exit 1;;
     esac
done

# Remove the html and build directories and then recreate the html/images/ directory
if [ -d html ]; then
   rm -r html/
fi

mkdir -p html
cp -r ../../docs/topics/images/ html/

echo ""
echo "********************************************"
echo " Building $CURRENT_GUIDE                "
echo "********************************************"
echo ""
echo "Building an asciidoctor version of the $CURRENT_GUIDE"
asciidoctor -t -dbook -a toc -o html/$CURRENT_GUIDE.html master.adoc

cd ..

echo "View the guide here: " file://$CURRENT_DIRECTORY/html/$CURRENT_GUIDE.html
