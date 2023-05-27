package net.thewinnt.cutscenes;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.thewinnt.cutscenes.path.Path;
import net.thewinnt.cutscenes.path.PathLike;

public class CutsceneType {
    public final int length;
    public final Path path;
    public final Path rotationProvider;

    public CutsceneType(PathLike path, Path rotationProvider, int length) {
        if (path instanceof Path pth) {
            this.path = pth;
        } else {
            this.path = new Path(path);
        }
        this.rotationProvider = rotationProvider;
        this.length = length;
    }

    public Vec3 getPathPoint(double point) {
        return path.getPoint(point);
    }

    public Vec3 getRotationAt(double point) {
        return rotationProvider.getPoint(point);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeInt(length);
        path.toNetwork(buf);
        rotationProvider.toNetwork(buf);
    }

    public static CutsceneType fromNetwork(FriendlyByteBuf buf) {
        int length = buf.readInt();
        Path path = Path.fromNetwork(buf, null);
        Path rotationProvider = Path.fromNetwork(buf, path);
        return new CutsceneType(path, rotationProvider, length);
    }

    public static CutsceneType fromJSON(JsonObject json) {
        int length = json.get("length").getAsInt();
        Path path = Path.fromJSON(json.getAsJsonObject("path"), null);
        Path rotation = Path.fromJSON(json.getAsJsonObject("rotation"), path);
        return new CutsceneType(path, rotation, length);
    }
}