package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.joml.Vector3f;
import raytrace.HitRecord;
import raytrace.Ray3D;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode
{
    /**
     * The name of the object instance that this leaf contains. All object instances are stored
     * in the scene graph itself, so that an instance can be reused in several leaves
     */
    protected String objInstanceName;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;

    protected String textureName;

    public LeafNode(String instanceOf,IScenegraph graph,String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;
    }



    /*
	 *Set the material of each vertex in this object
	 */
    @Override
    public void setMaterial(util.Material mat)
    {
        material = new util.Material(mat);
    }

    /**
     * Set texture ID of the texture to be used for this leaf
     * @param name
     */
    @Override
    public void setTextureName(String name)
    {
        textureName = name;
    }

    /*
     * gets the material
     */
    public util.Material getMaterial()
    {
        return material;
    }

    @Override
    public INode clone()
    {
        LeafNode newclone = new LeafNode(this.objInstanceName,scenegraph,name);
        newclone.setMaterial(this.getMaterial());
        return newclone;
    }


    /**
     * Delegates to the scene graph for rendering. This has two advantages:
     * <ul>
     *     <li>It keeps the leaf light.</li>
     *     <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
     * </ul>
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     * @throws IllegalArgumentException
     */
    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView) throws IllegalArgumentException
    {
        if (objInstanceName.length()>0)
        {
            context.drawMesh(objInstanceName,material,textureName,modelView.peek());
        }
    }

    @Override
    public List<HitRecord> raycast(Ray3D ray, Stack<Matrix4f> transforms) throws IllegalArgumentException {
        List<HitRecord> hits = new ArrayList<HitRecord>();
        List<HitRecord> objHits;
        switch(objInstanceName) {
            case "box":
                objHits = BoxRaycast(ray, transforms);
                if (objHits.size() > 0) {
                    hits.addAll(objHits);
                }
                break;
            case "sphere":
                objHits = SphereRaycast(ray, transforms);
                if (objHits.size() > 0) {
                    hits.addAll(objHits);
                }
                break;
            default:
                throw new IllegalArgumentException("RayTracing not yet supported for objects of type: " + objInstanceName);
        }
        return hits;
    }

    private List<HitRecord> BoxRaycast(Ray3D ray, Stack<Matrix4f> transforms) {
        List<HitRecord> hits = new ArrayList<HitRecord>();
        float maxT = 10000;
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
        float tx1 = (-0.5f - ray.getStart().x) / ray.getDirection().x;
        float tx2 = (-0.5f - ray.getStart().x) / ray.getDirection().x;
        float tminx = Math.min(tx1, tx2);
        float tmaxx = Math.max(tx1, tx2);
        float ty1 = (-0.5f - ray.getStart().y) / ray.getDirection().y;
        float ty2 = (-0.5f - ray.getStart().y) / ray.getDirection().y;
        float tminy = Math.min(ty1, ty2);
        float tmaxy = Math.max(ty1, ty2);
        float tz1 = (-0.5f - ray.getStart().z) / ray.getDirection().z;
        float tz2 = (-0.5f - ray.getStart().z) / ray.getDirection().z;
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
        Vector3f p = ray.getStart().add(ray.getDirection().mul(t));

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

        return hits;
    }

    private List<HitRecord> SphereRaycast(Ray3D ray, Stack<Matrix4f> transforms) {
        List<HitRecord> hits = new ArrayList<HitRecord>();
        float t = 0;
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

                /*
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

                */

                /*
                    Need Point of Intersection P, Vector N (normal at P), and List<Light> L, all in view coordinate system
                    To get N, apply the inverse transpose of M on N in object coordinate system
                 */
        Vector3f p = ray.getStart().add(ray.getDirection().mul(t));

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
        return hits;
    }
}
