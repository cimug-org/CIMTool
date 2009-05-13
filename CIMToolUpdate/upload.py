#! /usr/bin/python
#

from botoscripts.s3 import *
from botoscripts.util import *
from os import listdir
from os.path import isfile, join, splitext, basename
from zipfile import ZipFile, ZIP_DEFLATED
from re import search
from shutil import copyfile

def debug_mirror(*args):
	print "mirror", args

#mirror = debug_mirror

def findlast(folder, prefix="", suffix=".jar"):
	for f in sorted(listdir(folder), reverse=True):
		if f.startswith(prefix) and f.endswith(suffix):
			p = join(folder, f)
			if isfile(p):
				return p


def zipit(name, manifest):
	print "zipping:", name
	archive = ZipFile(name, "w", ZIP_DEFLATED)
	for m in manifest:
		archive.write(m)
	archive.close()

def zipmerge(dst, src, prefix):
	for info in src.infolist():
		if info.file_size:
			content = src.read(info.filename)
			info.filename = join(prefix, info.filename)
			# print "merging", info.filename, len(content), "bytes"
			dst.writestr(info, content)
	
def edit(text, **subs):
	for k in subs:
		text = text.replace(k, str(subs[k]))
	return text

def upload_files(bucket, manifest, prefix=""):
	for m in manifest:
		mirror(bucket, m, prefix + m)

BUCKET="files.cimtool.org"
DEVEL="development/"
ARCHIVE="CIMToolUpdate.zip"
ECLIPSE="CIMTool-Eclipse-VERSION.zip"
KIT="/home/share/eclipse-3.4.2-win32-kit.zip"

def manifest():
	yield findlast("features", "au.com.langdale.cimtoole.feature_")
	yield findlast("plugins", "au.com.langdale.cimtoole_")
	yield findlast("plugins", "au.com.langdale.cimtoole.help_")
	yield "site.xml"
	yield "artifacts.xml"
	yield "content.xml"
				
def lastversion():
	path = findlast("features", "au.com.langdale.cimtoole.feature_")
	return version(path)
	
def version(path):
	return search("_([0-9]+[.][0-9]+[.][0-9]+([.][0-9]+)?)[.]jar$", path).group(1)

def build_update_dist(archive_name, manifest):
	zipit(archive_name, manifest) 

def build_eclipse_dist(dist, manifest, kit=KIT):
	feature, plugin, docs = list(manifest)[:3]
	copyfile(kit, dist)
	archive = ZipFile(dist, "a", ZIP_DEFLATED)
	eclipsemerge(archive, feature)
	eclipseadd(archive, plugin)
	eclipseadd(archive, docs)
	archive.close()
	
def eclipsemerge(archive, jarfilename):
	component, ext = splitext(jarfilename)
	jarchive = ZipFile(jarfilename, 'r')
	zipmerge( archive, jarchive, join("eclipse", component))
	jarchive.close()
	
def eclipseadd(archive, jarfilename):
	archive.write( jarfilename, join( "eclipse", jarfilename))


def do_full(bucket_name=BUCKET):
	build_update_dist(ARCHIVE, manifest())
	eclipse_dist = ECLIPSE.replace("VERSION", lastversion())
	build_eclipse_dist(eclipse_dist, manifest())
	print "built:", ARCHIVE, eclipse_dist
	
	bucket = get_bucket(bucket_name)
	upload_files(bucket, manifest())
	mirror(bucket, ARCHIVE)
	mirror(bucket, eclipse_dist)
	
def do_build():
	build_update_dist(ARCHIVE, manifest())
	eclipse_dist = ECLIPSE.replace("VERSION", lastversion())
	build_eclipse_dist(eclipse_dist, manifest())
	print "built:", ARCHIVE, eclipse_dist

def do_interim(bucket_name=BUCKET):
	build_update_dist(ARCHIVE, manifest())
	bucket = get_bucket(bucket_name)
	#upload_files(bucket, manifest(), prefix=DEVEL)
	#mirror(bucket, ARCHIVE, DEVEL+ARCHIVE)
	upload_files(bucket, manifest())
	mirror(bucket, ARCHIVE)
	

def do_kit(bucket_name=BUCKET):
	eclipse_dist = ECLIPSE.replace("VERSION", lastversion())
	build_eclipse_dist(eclipse_dist, manifest())
	print "built:", ARCHIVE, eclipse_dist
	
	bucket = get_bucket(bucket_name)
	mirror(bucket, eclipse_dist)
        
        
def do_lastversion():
	print lastversion()

if __name__ == "__main__":
	shell(globals())
