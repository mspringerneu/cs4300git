package raytrace;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.w3c.dom.Text;
import util.TextureImage;

import java.util.Comparator;

/**
 * Created by mspringer on 11/21/16.
 */
public class HitRecord {
    private float tEnter;
    private float tExit;
    private Vector4f intersectionIn;
    private Vector4f normalIn;
    private Vector4f intersectionOut;
    private Vector4f normalOut;
    private util.Material material;
    private Matrix4f transform;
    private String textureName;
    private Vector4f texCoord;
    private String objType;
    private String intersectFace;

    public HitRecord(float tEnter, float tExit, Vector4f intersectionIn, Vector4f intersectionOut, Vector4f normalIn, Vector4f normalOut, util.Material material, Matrix4f transform, String textureName, String objType) {
        this.tEnter = tEnter;
        this.tExit = tExit;
        this.intersectionIn = intersectionIn;
        this.intersectionOut = intersectionOut;
        this.normalIn = normalIn;
        this.normalOut = normalOut;
        this.material = material;
        this.transform = transform;
        this.textureName = textureName;
        this.texCoord = new Vector4f();
        this.objType = objType;
        this.intersectFace = "";
        switch(objType) {
            case "box":
                this.intersectFace = getBoxFace(intersectionIn);
                this.texCoord = getTexCoordBox(intersectionIn);
                if (this.textureName.contains("-box")) {
                    this.texCoord = getTexCoordBoxWrap(this.texCoord);
                }
                break;
            case "sphere":
                if (this.textureName.contains("-box")) {
                    this.textureName = textureName.split("-box")[0];
                }
                this.texCoord = getTexCoordSphere(intersectionIn);
                break;
            default:
                break;
        }
    }

    private Vector4f getTexCoordBox(Vector4f intersection) {
        Vector4f texCoord = new Vector4f();
        float s, t;
        switch(this.intersectFace) {
            case "front":
                s = intersection.x + 0.5f;
                t = intersection.y + 0.5f;
                break;
            case "back":
                s = intersection.x - 0.5f;
                t = intersection.y + 0.5f;
                break;
            case "left":
                s = intersection.z + 0.5f;
                t = intersection.y + 0.5f;
                break;
            case "right":
                s = intersection.z - 0.5f;
                t = intersection.y + 0.5f;
                break;
            case "top":
                s = intersection.x + 0.5f;
                t = intersection.z - 0.5f;
                break;
            case "bottom":
                s = intersection.x + 0.5f;
                t = intersection.z - 0.5f;
                break;
            default:
                break;
        }

        return texCoord;
    }

    private Vector4f getTexCoordBoxWrap(Vector4f texCoord) {
        float s = texCoord.x;
        float t = texCoord.y;
        Vector4f offset = new Vector4f();
        switch(this.intersectFace) {
            case "front":
                offset.x = 0.25f;
                offset.y = 0.25f;
                offset.add(s * 0.25f, t * 0.25f, 0, 0);
                break;
            case "back":
                offset.x = 0.75f;
                offset.y = 0.25f;
                offset.add(s * 0.25f, t * 0.25f, 0, 0);
                break;
            case "left":
                offset.x = 0.0f;
                offset.y = 0.25f;
                offset.add(s * 0.25f, t * 0.25f, 0, 0);
                break;
            case "right":
                offset.x = 0.5f;
                offset.y = 0.25f;
                offset.add(s * 0.25f, t * 0.25f, 0, 0);
                break;
            case "top":
                offset.x = 0.25f;
                offset.y = 0.5f;
                offset.add(s * 0.25f, t * 0.25f, 0, 0);
                break;
            case "bottom":
                offset.x = 0.25f;
                offset.y = 0.0f;
                offset.add(s * 0.25f, 0.25f - (t * 0.25f), 0, 0);
                break;
            default:
                break;
        }
        return offset;
    }

    private String getBoxFace(Vector4f intersect) {
        float precision = 0.005f;
        if (Math.abs(intersect.x) - 0.5f < precision) {
            // right
            if (intersect.x > 0) {
                return "right";
            }
            // left
            else {
                return "left";
            }
        }
        else if (Math.abs(intersect.y) - 0.5f < precision) {
            // top
            if (intersect.y > 0) {
                return "top";
            }
            // bottom
            else {
                return "bottom";
            }
        }
        else if (Math.abs(intersect.z) - 0.5f < precision) {
            // front
            if (intersect.z > 0) {
                return "front";
            }
            // back
            else {
                return "back";
            }
        }
        else return "edge";
    }

    private Vector4f getTexCoordSphere (Vector4f intersection) {
        float theta, phi, s, t;
        t = intersection.y + 0.5f;
        /*
            For sphere of radius 1 and center (0,0,0)
            cross-section at some fixed y':
            x = cos(theta)sin(phi)
            y = cos(phi)
            z = -sin(phi)sin(theta)
         */
        phi = (float)Math.acos(intersection.y);
        theta = (float)Math.acos(intersection.x / Math.sin(phi));
        s = (float)(Math.toDegrees(theta) / 360f);

        Vector4f texCoord = new Vector4f(s,t,0f,0f);

        return texCoord;
    }

    public float getTEnter() {
        return this.tEnter;
    }

    public float getTExit() { return this.tExit; }

    public Vector4f getIntersectionIn() {
        return new Vector4f(this.intersectionIn);
    }
    public Vector4f getIntersectionOut() {
        return new Vector4f(this.intersectionOut);
    }
    public Vector4f getNormalIn() {
        return new Vector4f(this.normalIn);
    }
    public Vector4f getNormalOut() {
        return new Vector4f(this.normalOut);
    }
    public Matrix4f getTransform() {
        return new Matrix4f(this.transform);
    }
    public String getTextureName() { return this.textureName; }
    public Vector4f getTexCoord() { return new Vector4f(this.texCoord); }
    public String getObjType() { return this.objType; }

    public util.Material getMaterial() {
        return this.material;
    }

    /*Comparator for sorting a list of HitRecords by t*/
    public static Comparator<HitRecord> HitRecordTimeComparator = new Comparator<HitRecord>() {

        public int compare(HitRecord h1, HitRecord h2) {
            float t1 = h1.getTEnter();
            float t2 = h2.getTEnter();

            //ascending order
            return (int) (t1 - t2);
        }
    };
}
