#! /bin/sh
set -e
version=CIMTool${1:-$(date  +%Y%m%d)}
location=builds/$version

rm -rf $location $location.zip
mkdir -p $location

libs=""
for lib in $(xsltproc getlibs.xsl runtime.userlibraries)
do
    cp $lib $location
    libs="$libs $(basename $lib)"
done

sed -e "s/^Class-Path:.*/Class-Path: $libs/" < CIMTool.mf > CIMTool.mf.temp
mv CIMTool.mf.temp CIMTool.mf

sed -e "s/VERSION/$version/g" < README.txt > $location/README.txt
cp LICENSE.txt $location/LICENSE.txt

jar cfm $location/CIMTool.jar CIMTool.mf -C bin .

cd builds
zip -r $version.zip $version
