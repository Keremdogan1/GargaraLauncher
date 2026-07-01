import java.io.*;
import java.util.zip.GZIPOutputStream;
import java.util.Base64;

public class GenerateServersDat {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos));

        // TAG_Compound (root)
        dos.writeByte(10);
        dos.writeUTF("");

        // TAG_List ("servers")
        dos.writeByte(9);
        dos.writeUTF("servers");
        dos.writeByte(10); // List type: Compound
        dos.writeInt(1); // Length: 1

        // Server 1 Compound
        // name
        dos.writeByte(8);
        dos.writeUTF("name");
        dos.writeUTF("Gargara Sunucusu");
        // ip
        dos.writeByte(8);
        dos.writeUTF("ip");
        dos.writeUTF("130.61.80.148:25565");
        // acceptTextures (optional, usually 1 or 0, let's omit or add)
        // End Compound
        dos.writeByte(0);

        // End root Compound
        dos.writeByte(0);

        dos.close();

        byte[] data = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(data);
        System.out.println(base64);
    }
}
