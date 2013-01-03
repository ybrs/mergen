
def bytearray_to_str(a):
	return ''.join(chr(c) for c in a)

result = None
def process(e):
	# print "1 ============================"
	vals = []
	for i in e.getMessage().get():
		vals.append(bytearray_to_str(i))
	# print vals
	# print "5 ============================"	
	return 1