package org.infotoast.lorax.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructureReader {
    private StructureReader() { }

    public static Structure read(String name, Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        List<Structure.Entry> entries = new ArrayList<Structure.Entry>();
        String author = "Lorax Object Builder", description = "A BO5 Object";
        int version = 1, minHeight = 63, maxHeight = 318;
        boolean rotate = true;
        String line;
        int number = 0;
        while ((line = reader.readLine()) != null) {
            number++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try {
                if (!line.contains("(") && line.contains(":")) {
                    String[] metadata = line.split(":", 2);
                    String key = metadata[0], value = metadata[1].trim();
                    if ("Author".equals(key)) author = value;
                    else if ("Description".equals(key)) description = value;
                    else if ("Version".equals(key)) version = Integer.parseInt(value);
                    else if ("MinHeight".equals(key)) minHeight = Integer.parseInt(value);
                    else if ("MaxHeight".equals(key)) maxHeight = Integer.parseInt(value);
                    else if ("RotateRandomly".equals(key) || "RotateRandomy".equals(key)) {
                        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new IllegalArgumentException("Invalid boolean");
                        rotate = Boolean.parseBoolean(value);
                    } else throw new IllegalArgumentException("Unknown setting: " + key);
                    continue;
                }
                int open = line.indexOf('(');
                if (open < 1 || !line.endsWith(")")) throw new IllegalArgumentException("Expected command(args)");
                String command = line.substring(0, open).toUpperCase(Locale.ROOT);
                String[] args = line.substring(open + 1, line.length() - 1).split(",(?![^\\[\\]]*])", -1);
                if (args.length < 3) throw new IllegalArgumentException("Missing coordinates");
                Position position = new Position(Integer.parseInt(args[0].trim()), Integer.parseInt(args[1].trim()), Integer.parseInt(args[2].trim()));
                Structure.Kind kind;
                if ("B".equals(command) || "BLOCK".equals(command)) kind = Structure.Kind.BLOCK;
                else if ("RB".equals(command) || "RANDOMBLOCK".equals(command)) kind = Structure.Kind.RANDOM_BLOCK;
                else if ("GC".equals(command) || "GROUNDCHECK".equals(command)) kind = Structure.Kind.GROUND_CHECK;
                else if ("BC".equals(command) || "BLOCKCHECK".equals(command)) kind = Structure.Kind.BLOCK_CHECK;
                else throw new IllegalArgumentException("Unknown command: " + command);
                List<BlockState> blocks = new ArrayList<BlockState>();
                List<Integer> chances = new ArrayList<Integer>();
                if (kind == Structure.Kind.GROUND_CHECK) {
                    if (args.length != 3) throw new IllegalArgumentException("GroundCheck requires 3 arguments");
                } else if (kind == Structure.Kind.RANDOM_BLOCK) {
                    if (args.length < 5 || args.length % 2 != 1) throw new IllegalArgumentException("RandomBlock requires block/chance pairs");
                    int total = 0;
                    for (int i = 3; i < args.length; i += 2) {
                        blocks.add(BlockState.parse(args[i]));
                        int chance = Integer.parseInt(args[i + 1].trim());
                        if (chance < 0 || chance > 100) throw new IllegalArgumentException("Chance outside 0..100");
                        total += chance;
                        chances.add(chance);
                    }
                    if (total > 100) throw new IllegalArgumentException("RandomBlock chances exceed 100");
                } else {
                    if (args.length != 4) throw new IllegalArgumentException("Block/BlockCheck requires 4 arguments");
                    blocks.add(BlockState.parse(args[3]));
                }
                entries.add(new Structure.Entry(kind, position, blocks, chances));
            } catch (RuntimeException error) {
                throw new IOException(name + ":" + number + ": " + error.getMessage(), error);
            }
        }
        if (minHeight > maxHeight || entries.isEmpty()) throw new IOException(name + ": invalid height limits or empty structure");
        return new Structure(name, author, description, version, minHeight, maxHeight, rotate, entries);
    }
}
