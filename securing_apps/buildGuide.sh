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

if [ ! -d target ]; then
   echo "You must run 'python gitlab-conversion.py' to convert the content before you run this script."
   exit
fi

# Remove the html and build directories and then recreate the html/images/ directory
if [ -d target/html ]; then
-   rm -r target/html/
fi
if [ -d target/html ]; then
   rm -r target/html/
fi

mkdir -p html
cp -r target/images/ target/html/

echo ""
echo "********************************************"
echo " Building $CURRENT_GUIDE                "
echo "********************************************"
echo ""
echo "Building an asciidoctor version of the guide"
asciidoctor -t -dbook -a toc -o target/html/$CURRENT_GUIDE.html target/master.adoc

echo ""
echo "Building a ccutil version of the guide"
ccutil compile --lang en_US --format html-single --main-file target/master.adoc

cd ..

echo "View the asciidoctor build here: " file://$CURRENT_DIRECTORY/target/html/$CURRENT_GUIDE.html

if [ -d  $CURRENT_DIRECTORY/build/tmp/en-US/html-single/ ]; then
  echo "View the ccutil build here: " file://$CURRENT_DIRECTORY/build/tmp/en-US/html-single/index.html
  exit 0
else
  echo -e "${RED}Build using ccutil failed!"
  echo -e "${BLACK}See the log above for details."
  exit 1
fi
