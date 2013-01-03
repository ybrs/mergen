import os
import compileall
import processor

def dispatch(e):
	# compileall.compile_dir('.', force=True)
	# reload(processor)
	# print "dispatcher"
	processor.process(e)

