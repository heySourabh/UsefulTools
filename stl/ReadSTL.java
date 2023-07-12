package in.spbhat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

public class ReadSTL {

    private record Vector(float x, float y, float z) {
    }

    public record Triangle(Vertex v1, Vertex v2, Vertex v3) {
    }

    public record Vertex(int id, Vector location) {
    }

    public record SurfaceMesh(List<Vertex> vertices, List<Triangle> triangles) {
    }

    public static void main(String[] args) throws IOException {
        File file = new File("data/Utah_teapot_(solid).stl");
        SurfaceMesh surfaceMesh = readStlFile(file);
        writeAsVtk(surfaceMesh, "stl2vtk.vtk");
    }

    public static SurfaceMesh readStlFile(File stlFile) throws IOException {
        DataInputStream fileData = new DataInputStream(new BufferedInputStream(
                new FileInputStream(stlFile)));
        System.out.println("Reading STL file: " + stlFile.getName());
        throwExceptionIfAscii(fileData);

        ByteBuffer buffer = ByteBuffer.wrap(fileData.readNBytes(4))
                .order(ByteOrder.LITTLE_ENDIAN);
        int numTriangles = buffer.asIntBuffer().get();
        buffer.clear();

        System.out.println("Number of triangles: " + numTriangles);
        Map<Vector, Vertex> vectorToVertexMap = new HashMap<>();
        List<Triangle> triangles = new ArrayList<>(numTriangles);
        AtomicInteger vertexIndex = new AtomicInteger(0);
        Function<Vector, Vertex> vectorToVertexFunction = vec -> new Vertex(vertexIndex.getAndAdd(1), vec);

        int bytesPerTriangle = 12 * 3;
        buffer = ByteBuffer.allocate(bytesPerTriangle)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numTriangles; i++) {
            fileData.skipBytes(12); // skip Normal vector
            buffer.put(fileData.readNBytes(bytesPerTriangle)).rewind();
            FloatBuffer coordinate = buffer.asFloatBuffer();
            Vector v1 = new Vector(coordinate.get(), coordinate.get(), coordinate.get());     // 4*3 = 12 bytes
            Vector v2 = new Vector(coordinate.get(), coordinate.get(), coordinate.get());     // 4*3 = 12 bytes
            Vector v3 = new Vector(coordinate.get(), coordinate.get(), coordinate.get());     // 4*3 = 12 bytes
            fileData.skipBytes(2); // skip Attribute byte count

            Vertex vertex1 = vectorToVertexMap.computeIfAbsent(v1, vectorToVertexFunction);
            Vertex vertex2 = vectorToVertexMap.computeIfAbsent(v2, vectorToVertexFunction);
            Vertex vertex3 = vectorToVertexMap.computeIfAbsent(v3, vectorToVertexFunction);
            triangles.add(new Triangle(vertex1, vertex2, vertex3));
        }
        List<Vertex> vertices = new ArrayList<>(vectorToVertexMap.values());
        vertices.sort(Comparator.comparingInt(Vertex::id));

        return new SurfaceMesh(unmodifiableList(vertices), unmodifiableList(triangles));
    }

    private static void throwExceptionIfAscii(DataInputStream dis) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(dis.readNBytes(80));
        byte[] asciiIndicator = "solid ".getBytes(StandardCharsets.US_ASCII);
        byte[] fileStart = new byte[asciiIndicator.length];
        buffer.get(fileStart, 0, fileStart.length);
        if (Arrays.equals(fileStart, asciiIndicator)) {
            String errMsg = "ASCII files are not yet supported. Please save the STL file in BINARY format.";
            System.err.println(errMsg);
            throw new UnsupportedOperationException(errMsg);
        }
        buffer.rewind();
        System.out.println(new String(buffer.array(), StandardCharsets.US_ASCII));
    }

    private static void writeAsVtk(SurfaceMesh surfaceMesh, String vtkFilePath) throws IOException {
        List<Vertex> vertices = surfaceMesh.vertices();
        List<Triangle> triangles = surfaceMesh.triangles();
        System.out.println("Writing to vtk file: " + vtkFilePath);
        try (FileWriter vtkFile = new FileWriter(vtkFilePath)) {
            vtkFile.write("# vtk DataFile Version 2.0\n");
            vtkFile.write("STL to VTK\n");
            vtkFile.write("ASCII\n");
            vtkFile.write("DATASET POLYDATA\n");
            vtkFile.write("POINTS %d float\n".formatted(vertices.size()));
            for (Vertex vertex : vertices) {
                vtkFile.write("%f %f %f\n".formatted(vertex.location().x(),
                        vertex.location().y(),
                        vertex.location().z()));
            }
            vtkFile.write("VERTICES 1 %d\n".formatted(vertices.size() + 1));
            vtkFile.write("%d".formatted(vertices.size()));
            for (Vertex vertex : vertices) {
                vtkFile.write(" %d".formatted(vertex.id()));
            }
            vtkFile.write("\n");
            vtkFile.write("POLYGONS %d %d\n".formatted(triangles.size(), triangles.size() * 4));
            for (Triangle triangle : triangles) {
                vtkFile.write("3 %d %d %d\n".formatted(triangle.v1().id(), triangle.v2().id(), triangle.v3().id()));
            }
        }
    }
}