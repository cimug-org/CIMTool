#! /bin/sh
#
# package CIMTool with an eclipse platform distribution
#
set -x

if [ ! -f site.xml ]
then
    echo "This does not seem to be an eclipse update site"
    exit 1
fi

REPO=$PWD
ASSEMBLY=$PWD/"Assembly"
JRE="/extra/distfiles/sun-jre-v6-win32.zip"
PLATFORM="/extra/distfiles/eclipse-platform-3.5-win32.zip"
ECLIPSE="/extra/share/eclipse-rcp-galileo/eclipse"
PACKAGE=$PWD/${1:-CIMTool-Eclipse.zip}


make_package() {

rm -rf $ASSEMBLY $PACKAGE
mkdir $ASSEMBLY
unzip $PLATFORM -d $ASSEMBLY
unzip $JRE -d $ASSEMBLY/eclipse

patch $ASSEMBLY/eclipse/eclipse.ini << EOF
--- Assembly/eclipse/eclipse.ini        2009-06-12 08:33:48.000000000 +1000
+++ ../Assembly/eclipse/eclipse.ini     2009-08-05 16:10:20.000000000 +1000
@@ -4,6 +4,8 @@
 plugins/org.eclipse.equinox.launcher.win32.win32.x86_1.0.200.v20090519
 -showsplash
 org.eclipse.platform
+-perspective
+au.com.langdale.cimtoole.CIMToolPerspective
 --launcher.XXMaxPermSize
 256m
 -vmargs
EOF

$ECLIPSE \
    -application org.eclipse.equinox.p2.director \
   -repository file:$REPO \
   -installIU au.com.langdale.cimtoole.feature.feature.group \
   -destination $ASSEMBLY/eclipse \
   -profile PlatformProfile

( cd $ASSEMBLY && zip -r $PACKAGE eclipse )


}

mirror_site() {

# this does not seem to recreate the content.jar file

rm -rf $ASSEMBLY
mkdir $ASSEMBLY
$ECLIPSE \
    -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication \
    -source file:$REPO \
    -destination file:$ASSEMBLY/site

( cd $ASSEMBLY && zip -r $ARCHIVE  * )

}

make_site() {

rm -f $ARCHIVE
( cd $REPO && zip -r -n jar $ARCHIVE site.xml content.jar artifacts.jar plugins features )

}


echo eclipse package is $PACKAGE 
make_package

#echo archive site is $ARCHIVE
#make_site
