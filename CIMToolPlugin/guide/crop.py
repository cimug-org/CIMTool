from PIL.Image import open
from os import listdir
from os.path import isfile, join
from pprint import pprint

def get_sizes(top, suffix):
	for f in listdir(top):
		p = join(top, f)
		if p.endswith(suffix) and isfile(p):
			try:
				i = open(p)
			except IOError:
				pass
			yield f, i.size

def crop(src, dst, sizes):
	for f, (w, h) in sizes:
		p = join(src, f)
		try:
			i = open(p)
		except IOError:
			print p+": could not read image"

		w1, h1 = i.size
		if( w1 > w or h1 > h):
			j = i.crop((0, 0, w, h))
			j.save(join(dst,f))
			#print p+": cropped %d, %d" %(w1-w, h1-h)
		else:
			print p+": not changed"

if __name__ == '__main__':
	crop('CIMTool', 'newimage', get_sizes('image', '.png'))
