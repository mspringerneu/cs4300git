import com.jogamp.opengl.GL;
import org.joml.Vector4f;
import util.IVertexData;
import util.PolygonMesh;

import java.util.*;

/**
 * Created by mspringer on 9/28/16.
 */
public class Tower {
    private float BASE_DIMENSION = 10f;
    private int id;
    public Stack<Disk> disks;
    public List<Disk> diskList;
    public List<PolygonMesh> mesh;
    public List<Map<String, String>> shaderToVertexAttribute;

    public Tower(int id) {
        this.id = id + 1;
        generateMesh();
    }

    public int getId() {
        return this.id;
    }

    public Disk getTopDisk() {
        return this.disks.peek();
    }

    public void popTopDisk() {
        try {
            this.disks.pop();
        }
        catch (EmptyStackException e) {
            e.printStackTrace();
        }
    }

    public void addNewDisk(Disk d) {
        disks.push(d);
    }

    public void addNewDiskToList(Disk d) {
        this.diskList.add(d);
    }

    private void generateMesh() {
        List<Vector4f> colors = new ArrayList<Vector4f>();

        colors.add(new Vector4f(1, 0, 0, 1)); //red
        colors.add(new Vector4f(0, 1, 0, 1)); //green
        colors.add(new Vector4f(0, 0, 1, 1)); //blue
        colors.add(new Vector4f(0, 0, 0, 1)); //black

        List<Vector4f> positions = new ArrayList<Vector4f>();

        /** BASE **/
        // bottom left
        positions.add(new Vector4f(-BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
        // bottom right
        positions.add(new Vector4f(BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
        // top right
        positions.add(new Vector4f(BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));
        // top left
        positions.add(new Vector4f(-BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));


        List<IVertexData> vertexData = new ArrayList<IVertexData>();
        VertexAttribWithColorProducer producer = new VertexAttribWithColorProducer();
        for (int i = 0; i < positions.size(); i++) {
            IVertexData v = producer.produce();
            v.setData("position", new float[]{positions.get(i).x,
                    positions.get(i).y,
                    positions.get(i).z,
                    positions.get(i).w});
            v.setData("color", new float[]{colors.get(this.id % colors.size()).x,
                    colors.get(this.id % colors.size()).y,
                    colors.get(this.id % colors.size()).z,
                    colors.get(this.id % colors.size()).w});
            vertexData.add(v);
        }

                /*
                We now generate a series of indices.
                These indices will be for the above list
                of vertices. For example we want to use
                the above list to draw triangles.
                The first triangle will be created from
                vertex numbers 0, 1 and 2 in the list above
                (indices begin at 0 like arrays). The second
                triangle will be created from vertex numbers
                0, 2 and 3. Therefore we will create a list
                of indices {0,1,2,0,2,3}.

                What is the advantage of having a second
                list? Vertices are often shared between
                triangles, and having a separate list of
                indices avoids duplication of vertex data
                 */
        List<Integer> indices = new ArrayList<Integer>();
        indices.add(0);
        indices.add(1);
        indices.add(2);

        indices.add(0);
        indices.add(2);
        indices.add(3);

        //now we create a polygon mesh object
        for (int i = 0; i < 2; i++) {
            PolygonMesh mesh = new PolygonMesh();


            mesh.setVertexData(vertexData);
            mesh.setPrimitives(indices);

                /*
                It turns out, there are several ways of
                reading the list of indices and interpreting
                them as triangles.

                The first, simplest (and the one we have
                assumed above) is to just read the list of
                indices 3 at a time, and use them as triangles.
                In OpenGL, this is the GL_TRIANGLES mode.

                If we wanted to draw lines by reading the indices
                two at a time, we would specify GL_LINES.

                In any case, this "mode" and the actual list of
                indices are related. That is, decide which mode
                you want to use, and accordingly build the list
                of indices.
                 */

            mesh.setPrimitiveType(GL.GL_TRIANGLES);
            mesh.setPrimitiveSize(3);

                /*
                now we create an ObjectInstance for it
                The ObjectInstance encapsulates a lot of the
                 OpenGL-specific code to draw this object
                 */

                /* so in the mesh, we have some attributes for each vertex. In the shader
                we have variables for each vertex attribute. We have to provide a mapping
                between attribute name in the mesh and corresponding shader variable name.
                This will allow us to use PolygonMesh with any shader program, without
                assuming that the attribute names in the mesh and the names of shader variables
                will be the same.

                We create such a shader variable -> vertex attribute mapping now
                 */
            Map<String, String> shaderVertexAttr = new HashMap<String, String>();

            //currently there are two per-vertex attributes: position and color
            shaderVertexAttr.put("vPosition", "position");
            shaderVertexAttr.put("vColor", "color");

            this.mesh.add(mesh);
            this.shaderToVertexAttribute.add(shaderVertexAttr);
        }
    }
}
