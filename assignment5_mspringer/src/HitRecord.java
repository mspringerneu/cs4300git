import org.joml.Vector3f;
import util.Material;

/**
 * Created by mspringer on 11/21/16.
 */
public class HitRecord {
    private float t;
    private Vector3f intersection;
    private Vector3f normal;
    private Material material;

    public HitRecord(float t, Vector3f intersection, Vector3f normal, Material material) {
        this.t = t;
        this.intersection = intersection;
        this.normal = normal;
        this.material = material;
    }
}
