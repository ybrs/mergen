import pystacia

noise = '''iso=32; rone=rand(); rtwo=rand();
myn=sqrt(-2*ln(rone))*cos(2*Pi*rtwo); myntwo=sqrt(-2*ln(rtwo))*
cos(2*Pi*rone); pnoise=sqrt(p)*myn*sqrt(iso)* 
channel(4.28,3.86,6.68,0)/255; max(0,p+pnoise)''' 


if __name__ == "__main__":
    image = pystacia.read('dont_panic.jpg')
    image.fx('''iso=32; rone=rand(); rtwo=rand();
myn=sqrt(-2*ln(rone))*cos(2*Pi*rtwo); myntwo=sqrt(-2*ln(rtwo))*
cos(2*Pi*rone); pnoise=sqrt(p)*myn*sqrt(iso)* 
channel(4.28,3.86,6.68,0)/255; max(0,p+pnoise)''')
    image.write("cloned.jpg")