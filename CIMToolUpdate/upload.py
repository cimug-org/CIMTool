#! /usr/bin/python
# -*- coding: utf-8 -*-
#

from botoscripts.s3 import *
from botoscripts.util import *
from os import listdir, unlink, rename
from os.path import isfile, join, splitext, basename
from re import search
from subprocess import check_call

BUCKET="files.cimtool.org"
DEVEL="development/"
ARCHIVE="CIMToolUpdate.zip"
ECLIPSE="CIMTool-Eclipse-VERSION.zip"

def manifest():
	yield findlast("features", "au.com.langdale.cimtoole.feature_")
	yield findlast("plugins", "au.com.langdale.cimtoole_")
	yield findlast("plugins", "au.com.langdale.cimtoole.help_")
	yield findlast("plugins", "au.com.langdale.kena_")
	yield findlast("plugins", "au.com.langdale.rcputil_")
	yield "site.xml"
	yield "artifacts.jar"
	yield "content.jar"

def findlast(folder, prefix="", suffix=".jar"):
	for f in sorted(listdir(folder), reverse=True):
		if f.startswith(prefix) and f.endswith(suffix):
			p = join(folder, f)
			if isfile(p):
				return p


def upload_files(bucket, manifest, prefix=""):
	for m in manifest:
		mirror(bucket, m, prefix + m)
				
def lastversion():
	path = findlast("features", "au.com.langdale.cimtoole.feature_")
	return version(path)
	
def version(path):
	return search("_([0-9]+[.][0-9]+[.][0-9]+)([.][0-9]+)?[.]jar$", path).group(1)

def do_full(bucket_name=BUCKET):
	do_build()
	do_upload()

def do_upload(bucket_name=BUCKET):
	eclipse_dist = ECLIPSE.replace("VERSION", lastversion())
	bucket = get_bucket(bucket_name)
	upload_files(bucket, manifest())
	mirror(bucket, ARCHIVE)
	mirror(bucket, eclipse_dist)
	
def do_build():
	eclipse_dist = ECLIPSE.replace("VERSION", lastversion())
	check_call([ "sh", "package.sh", eclipse_dist ])
	
	tmp = "_" + ARCHIVE
	if isfile(tmp):
	    unlink(tmp)
	check_call([ "zip", "-r", "-n", "jar", tmp ] + list(manifest()))
	rename( tmp, ARCHIVE)
	
	print "built:", ARCHIVE, eclipse_dist
        
def do_lastversion():
	print lastversion()

if __name__ == "__main__":
	shell(globals())
