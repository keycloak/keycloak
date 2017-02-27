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

echo ""
echo "********************************************"
echo " Building $CURRENT_GUIDE                    "
echo "********************************************"
if [ ! -d target ]; then
   echo "You must run 'python gitlab-conversion.py' to convert the content before you run this script."
   exit
fi

# Remove the guide directory path from the master.adoc file as it is not needed
echo ""
echo "***************************************************************************************"
echo "Removing the guide directory path from the master.adoc file as it is not needed."
echo "NOTE: The guide directory path should probably be removed from the SUMMARY.adoc file,"
echo "but since we do not know if it will break the upstream build, we are doing this here. "
echo "If it can not be removed from the SUMMARY.adoc file, this should really be done in the "
echo "Python script because otherwise you must run this script before porting content!"
echo "***************************************************************************************"
echo ""
find . -name 'master.adoc' -print | xargs sed -i "s/include::$CURRENT_GUIDE\//include::/g"

# Remove the html and build directories and then recreate the html/images/ directory
if [ -d target/html ]; then
   rm -r target/html/
fi
if [ -d target/html ]; then
   rm -r target/html/
fi

mkdir -p html
cp -r target/images/ target/html/

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
