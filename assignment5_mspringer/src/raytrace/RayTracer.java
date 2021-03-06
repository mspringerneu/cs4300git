package raytrace;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import java.util.Stack;

/**
 * Created by mspringer on 11/17/16.
 */

public class RayTracer {
    private int height;
    private int width;
    private Vector3f start;
    private float theta;
    private float defaultMaxT = 10000.0f;
    private float maxT;
    private BufferedImage buffer;

    public RayTracer(int height, int width, Vector3f start, float theta) {
        this.height = height;
        this.width = width;
        this.start = start;
        this.theta = theta;
        this.maxT = defaultMaxT;
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void trace(Stack<Matrix4f> transforms) {
        for(int i =0;i<this.width;i++) {
            for (int j = 0; j < this.height; j++) {
                /*
                    Ray(start S, vector V)
                    create ray in view coordinates
                    S: 0,0,0 always! (when in view coordinate system)
                    going through near plane pixel (i,j)
                    So 3D location of that pixel in view coordinates is
                    V = Vector(x, y, z)
                        x = i-width/2
                        y = j-height/2
                        z = -0.5*height/tan(0.5*FOVY)
                */
                Vector3f v = new Vector3f(
                        (i-(this.width/2)) - start.x,
                        (j-(this.height/2)) - start.y,
                        (float)(((-0.5f * this.height)/Math.tan(theta/2)) - start.z));

                /*
                    Given ray R and object O, does R hit O?
                    If so: where?
                 */
                /*
                    Equation of a plane P: ax + by + cz + d = 0
                    Px = Sx + tVx
                    Py = Sy + tVy
                    Pz = Sz + tVz
                    t = -(aSx + bSy + cSz + d) / (aVx + bVy + cVz)

                    If ray || to plane P, divide by zero error
                 */
                /*
                    For box centered at 0 with dimensions (1,1,1)
                        front plane:    -0.5 <= x <= 0.5 ; -0.5 <= y <= 0.5 ;         z =  0.5
                        rear plane:     -0.5 <= x <= 0.5 ; -0.5 <= y <= 0.5 ;         z = -0.5
                        left plane:             x = -0.5 ; -0.5 <= y <= 0.5 ; -0.5 <= z <= 0.5
                        right plane:            x =  0.5 ; -0.5 <= y <= 0.5 ; -0.5 <= z <= 0.5
                        top plane:      -0.5 <= x <= 0.5 ;         y =  0.5 ; -0.5 <= z <= 0.5
                        bottom plane:   -0.5 <= x <= 0.5 ;         y = -0.5 ; -0.5 <= z <= 0.5

                    tx1 = (-0.5 - Sx) / Vx
                    tx2 = (-0.5 - Sx) / Vx
                    tminx = min(tx1, tx2)
                    tmaxx = max(tx1, tx2)
                    ty1 = (-0.5 - Sy) / Vy
                    ty2 = (-0.5 - Sy) / Vy
                    tminy = min(ty1, ty2)
                    tmaxy = max(ty1, ty2)
                    tz1 = (-0.5 - Sz) / Vz
                    tz2 = (-0.5 - Sz) / Vz
                    tminz = min(tz1, tz2)
                    tmaxz = max(tz1, tz2)
                    tmin = max(tminx, tminy, tminz)
                    tmax = min(tmaxx, tmaxy, tmaxz)
                 */
                float tx1 = (-0.5f - start.x) / v.x;
                float tx2 = (-0.5f - start.x) / v.x;
                float tminx = Math.min(tx1, tx2);
                float tmaxx = Math.max(tx1, tx2);
                float ty1 = (-0.5f - start.y) / v.y;
                float ty2 = (-0.5f - start.y) / v.y;
                float tminy = Math.min(ty1, ty2);
                float tmaxy = Math.max(ty1, ty2);
                float tz1 = (-0.5f - start.z) / v.z;
                float tz2 = (-0.5f - start.z) / v.z;
                float tminz = Math.min(tz1, tz2);
                float tmaxz = Math.max(tz1, tz2);
                float tmin = Math.max(tminx, Math.max(tminy, Math.max(tminz, -maxT)));
                float tmax = Math.min(tmaxx, Math.min(tmaxy, Math.min(tmaxz, maxT)));
                float t;

                if (tmin != maxT && tmin > 0) {
                    t = tmin;
                }
                else {
                    t = tmax;
                }

                /*
                    Need Point of Intersection P, Vector N (normal at P), and List<Light> L, all in view coordinate system
                    To get N, apply the inverse transpose of M on N in object coordinate system
                 */
                Vector3f p = start.add(v.mul(t));

                /*
                    Equation of sphere with center C (Cx,Cy,Cz) and radius r: (X-Cx)^2 + (Y-Cy)^2 + (Z-Cz)^2 = r^2

                    // to get t:
                    At^2 + Bt + C = 0

                    // For any sphere
                    A = Vx^2 + Vy^2 + Vz^2
                    B = 2Vx(Sx-Cx) + 2Vy(Sy-Cy) + 2Vz(Sz-Cz)
                    C = (Sx-Cx)^2 + (Sy-Cy)^2 + (Sz-Cz)^2 - r^2

                    P = S + tV
                    N = P - C

                    // For sphere centered at origin with radius = 1
                    A = Vx^2 + Vy^2 + Vz^2
                    B = 2(VxSx + VySy + VzSz)
                    C = Sx^2 + Sy^2 + Sz^2 - 1

                    P = S + tV
                    N = P

                    // Catch cases:
                    A = 0 : cannot happen without other mistakes in code
                    B^2 - 4AC < 0 : means the ray does not hit the sphere
                 */

                /*
                    Textures
                    -PI/2 <= phi <= PI/2
                    0 <= theta <= 2PI

                    X = r*cos(theta)*cos(phi)
                    Y = r*sin(phi
                    Z = r*sin(theta)*cos(phi)

                    Xo = cos(theta)*cos(phi)
                    Yo = sin(phi
                    Zo = sin(theta)*cos(phi)
                    phi = sin^-1(Yo)  ==>  t = (phi + PI/2)/PI
                    theta = tan^-1(-Zo/Xo)  ==> s = theta/2PI
                 */

                /*
                    Shadows
                        Color at point P due to light i:
                        C(P) = (i=0 SIGMA n) { Si }
                        Si = 1 --> not in shadow
                        Si = 0 --> in shadow

                    To determine if a point P is in shadow from light i:
                        Cast a Ray from P to i.position
                 */



                //get color in (r,g,b)
                int r, g, b;
                if ((i + j) % 10 < 5)
                    r = g = b = 0;
                else
                    r = g = b = 255;
                this.buffer.setRGB(i, j, new Color(r, g, b).getRGB());
            }
        }

        OutputStream outStream = null;

        try {
            outStream = new FileOutputStream("output/raytrace.png");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }

        try {
            ImageIO.write(this.buffer, "png", outStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }
    }

    public void setMaxT(float t) {
        this.maxT = t;
    }

    public void resetMaxT() {
        this.maxT = this.defaultMaxT;
    }
}

