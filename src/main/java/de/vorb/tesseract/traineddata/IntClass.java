package de.vorb.tesseract.traineddata;

import static de.vorb.tesseract.traineddata.IntTemplates.PROTOS_PER_PROTO_SET;
import static de.vorb.tesseract.traineddata.IntTemplates.WERDS_PER_CONFIG_VEC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntClass {
    private final int numProtos;
    private final ArrayList<ProtoSet> protoSets;
    private final short[] protoLengths;
    private final int[] configLengths;
    private final int fontSetId;

    private IntClass(int numProtos, ArrayList<ProtoSet> protoSets,
            short[] protoLengths,
            int[] configLengths, int fontSetId) {
        this.numProtos = numProtos;
        this.protoSets = protoSets;
        this.protoLengths = protoLengths;
        this.configLengths = configLengths;
        this.fontSetId = fontSetId;
    }

    public int getNumProtos() {
        return numProtos;
    }

    public List<ProtoSet> getProtoSets() {
        return Collections.unmodifiableList(protoSets);
    }

    public short[] getProtoLengths() {
        return protoLengths;
    }

    public int[] getConfigLengths() {
        return configLengths;
    }

    public int getFontSetId() {
        return fontSetId;
    }

    public static IntClass readFromBuffer(ReadableByteBuffer buf)
            throws IOException {
        if (!buf.hasNext(4)) {
            throw new IOException("invalid int class header");
        }

        // see intproto.cpp@966
        final int numProtos = buf.getShort() & 0xFFFF;
        final int numProtoSets = buf.get() & 0xFF;
        final int numConfigs = buf.get() & 0xFF;

        // read config lengths
        final int[] configLengths = new int[numConfigs];
        for (int i = 0; i < numConfigs; i++) {
            if (!buf.hasNext(2)) {
                throw new IOException("not enough config lengths");
            }

            configLengths[i] = buf.getShort() & 0xFFFF;
        }

        // read proto lengths
        final short[] protoLengths = new short[numProtoSets
                * PROTOS_PER_PROTO_SET];
        for (int i = 0; i < protoLengths.length; i++) {
            if (!buf.hasNext(1)) {
                throw new IOException("not enough proto lengths");
            }

            protoLengths[i] = (short) (buf.get() & 0xFF);
        }

        // read proto sets
        final ArrayList<ProtoSet> protoSets = new ArrayList<>(numProtoSets);
        for (int i = 0; i < numProtoSets; i++) {
            // read pruner
            final int[][][] protoPruner = new int[3][64][2];
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 64; y++) {
                    for (int z = 0; z < 2; z++) {
                        if (!buf.hasNext(1)) {
                            throw new IOException("not enough proto pruners");
                        }

                        protoPruner[x][y][z] = buf.getInt();
                    }
                }
            }

            final ArrayList<IntProto> protos =
                    new ArrayList<>(PROTOS_PER_PROTO_SET);
            for (int x = 0; x < PROTOS_PER_PROTO_SET; x++) {
                if (!buf.hasNext(4)) {
                    throw new IOException("not enough protos");
                }

                // get prototype information
                final byte a = buf.get();
                final byte b = buf.get();
                final byte c = buf.get();
                final byte angle = buf.get();

                final int[] configs = new int[WERDS_PER_CONFIG_VEC];
                for (int y = 0; y < WERDS_PER_CONFIG_VEC; y++) {
                    if (!buf.hasNext(4)) {
                        throw new IOException("not enough prototype configs");
                    }

                    configs[y] = buf.getInt();
                }

                protos.add(new IntProto(a, b, c, angle, configs));
            }

            protoSets.add(new ProtoSet(protoPruner, protos));
        }

        final int fontSetId = buf.getInt();

        return new IntClass(numProtos, protoSets, protoLengths, configLengths,
                fontSetId);
    }
}
