#version 400 core

out vec4 fColor;
uniform vec2 dims;
uniform vec2 center;
uniform float scale;
uniform int maxiter;

void main()
{
	int i;
	dvec2 z = dvec2(gl_FragCoord[0],gl_FragCoord[1]);
	//convert to the actual window coordinates specified by center and scale

	z.x = scale*(z.x/dims.x - 0.5)+ center.x ;
	z.y = scale*(z.y/dims.y - 0.5)+ center.y ;
	dvec2 cl = z; /* for mandelbrot */
//	dvec2 cl = vec2(-0.7017,-0.3842); /* for julia 1*/
//		dvec2 cl = dvec2(-0.8,0.156); /* for julia 2*/
//		dvec2 cl = dvec2(-0.4,0.6); /* for julia 3*/
	vec3 colors[7];

	colors[0] = vec3(0,0,0);
	colors[1] = vec3(0,0,1);
	colors[2] = vec3(0,1,1);
	colors[3] = vec3(0,1,0);
	colors[4] = vec3(1,1,0);
	colors[5] = vec3(1,0,0);
	colors[6] = vec3(0,0,0);

//	z = dvec2(0,0); //for mandelbrot
	
	for (i=0;i<maxiter;i++)
	{
		double x;
		double y;

		x = (z.x*z.x - z.y*z.y) + cl.x;
	//	y = (z.x*z.y + z.y*z.x) + cl.y;
	y = 2*z.x*z.y + cl.y;
	

		if ((x*x+y*y)>4.0)
			break;

		z.x = x;
		z.y = y;
	}

    if (i==maxiter)
		fColor = vec4(0,0,0,1);
	else
	{
	//	fColor = vec4(1,1,1,1);
	//	fColor = vec4(1.0*i/maxiter,1.0*i/maxiter,1.0*i/maxiter,1);
		float incr = 6.0*i/maxiter;
		if (incr>=6)
		{
			incr = 5.999;
		}
		
		int c = int(floor(incr));

		fColor = vec4(mix(colors[c],colors[c+1],incr-c),1.0);
		
		
	}
	
	
}
