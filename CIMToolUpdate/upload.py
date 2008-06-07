from boto.s3.connection import S3Connection
from boto.s3.key import Key
from os import listdir
from os.path import isfile, join, split, splitext
from zipfile import ZipFile, ZIP_DEFLATED
from re import escape, search
from shutil import copyfile

BUCKET="files.cimtool.org"
ARCHIVE="CIMToolUpdate.zip"

def read(filename):
	f = file(filename)
	try:
		return f.read()
	finally:
		f.close()

def write(filename, contents):
	f = file(filename, "w")
	try:
		f.write(contents)
	finally:
		f.close()

def credentials():
	access = read("aws/access-key").strip()
	secret = read("aws/secret-key").strip()
	return access, secret

def connect():
	access, secret = credentials()
	return S3Connection( access, secret )

def get_bucket(name):
	return connect().get_bucket(name)

def list_keys(bucket):
	for key in bucket:
		print key.name

def mirror(bucket, pathname):
	print "uploading", bucket.name + "/" + pathname
	handle = Key(bucket)
	handle.key = pathname
	handle.set_contents_from_filename(pathname)
	handle.set_acl('public-read')

def findlast(folder, suffix=".jar"):
	for f in sorted(listdir(folder), reverse=True):
		if f.endswith(suffix):
			p = join(folder, f)
			if isfile(p):
				return p
				
def lastversion():
	path = findlast("features")
	return version(path)
	
def version(path):
	return search("_([0-9]+[.][0-9]+[.][0-9]+)[.]jar$", path).group(1)

def do_lastversion():
	print lastversion()

class Manifest(object):
	def __iter__(self):
		yield findlast("features")
		yield findlast("plugins")
		yield "site.xml"

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
			print "merging", info.filename, len(content), "bytes"
			dst.writestr(info, content)

def upload_files(bucket, archive_name=ARCHIVE, manifest=Manifest()):
	zipit(archive_name, manifest) 
	mirror(bucket, archive_name)
	for m in manifest:
		mirror(bucket, m)

def build_eclipse_dist(kit="eclipse-win32-kit.zip", dist="CIMTool-Eclipse-VERSION.zip", manifest=Manifest()):
	feature, plugin = list(manifest)[:2]
	dist = dist.replace("VERSION", version(feature))
	copyfile(kit, dist)
	archive = ZipFile(dist, "a", ZIP_DEFLATED)
	for jarfile in feature, plugin:
		component, ext = splitext(jarfile)
		jarchive = ZipFile(jarfile, 'r')
		zipmerge( archive, jarchive, join("eclipse", component))
		jarchive.close()
	archive.close()
	return dist

def do_build_eclipse():
	print build_eclipse_dist()
	
def do_all(bucket_name=BUCKET):
	bucket = get_bucket(bucket_name)
	upload_files(bucket)
	mirror(bucket, build_eclipse_dist())
	update_wiki()
	list_keys(bucket)

def do_mirror(file_name, bucket_name=BUCKET):
	print 'mirror', file_name, bucket_name
	bucket = get_bucket(bucket_name)
	mirror(bucket, file_name)

def do_list(bucket_name=BUCKET):
	bucket = get_bucket(bucket_name)
	list_keys(bucket)
	
def edit(text, **subs):
	for k in subs:
		text = text.replace(k, str(subs[k]))
	return text
	
def do_edit():
	print edit(read("DownLoad.txt"), VERSION=lastversion())
	
from urllib2 import build_opener, HTTPPasswordMgrWithDefaultRealm, HTTPBasicAuthHandler, HTTPDigestAuthHandler, HTTPCookieProcessor
from urllib import urlencode
from sys import stdout

DOMAIN="cimtool.org"
PREFIX="http://" + DOMAIN + "/cimtool/"
PLAIN_PAGE=PREFIX + "wiki/%s"
EDIT_PAGE=PREFIX + "wiki/%s?action=edit"
TEXT_PAGE=PREFIX + "wiki/%s?format=txt"
LOGIN_PAGE=PREFIX + "login"

EDIT_FORM=dict(
		action="edit", 
		tags="", 
		comment="", 
		save="Submit changes"
)

CREATE_FORM=dict(
		action="edit",
		submit="Create this page",
)

VERSION="""<input type="hidden" name="version" value="VERSION" />"""
PATTERN = escape(VERSION).replace("VERSION", "([0-9]+)")

def extract_version(form):
	match = search( PATTERN, form)
	return match.group(1)
	
def wikipass():
	return read("wikipass.conf").split()

def replace_wiki_page(session, username, page, content):
	post(session, LOGIN_PAGE)
	form = post(session, EDIT_PAGE % page )
	text = post(session, TEXT_PAGE % page )
	version = extract_version(form)
	post(session, PLAIN_PAGE % page, dict( EDIT_FORM, author=username, text=content, version=version))
	return text

def create_session(uri, domain, username, password ):
	passwords = HTTPPasswordMgrWithDefaultRealm()
	passwords.add_password(  domain, uri, username, password)
	return build_opener(HTTPDigestAuthHandler(passwords), HTTPBasicAuthHandler(passwords), HTTPCookieProcessor())

def post(session, uri, postdata=None):
	try:
		if postdata != None:
			print "POST", uri, postdata
			postdata = urlencode(postdata)
		else:
			print "GET", uri
		f = session.open(uri, postdata)
	except Exception, ex:
		print uri, ex
		raise

	try:
		return f.read()
	finally:
		f.close()

def update_wiki_page(session, username, root, **subs):
	path, name = split(root)
	print "updating wiki page", name
	content = edit(read(root + ".txt"), **subs)
	previous = replace_wiki_page(session, username, name, content)
	write( root + ".bak", previous)

def update_wiki():
	username, password = wikipass()
	session = create_session(PREFIX, DOMAIN, username, password)
	update_wiki_page( session, username, "Download", VERSION=lastversion())
	update_wiki_page( session, username, "../CIMToolFeature/ChangeLog" )
	update_wiki_page( session, username, "../CIMToolFeature/GettingStarted" )

def do_update_wiki():
	update_wiki()


if __name__ == "__main__":
	from sys import argv
	if len(argv) < 2:
		do_all()
	else:
		arg = argv[1]
		action = "do_" + arg
		if action in globals():
			func = globals()[action]
			func(*argv[2:])
		else:
			print "no such command:", arg
