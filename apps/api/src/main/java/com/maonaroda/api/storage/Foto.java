package com.maonaroda.api.storage;

import java.io.*;
import javax.imageio.ImageIO;

/** Recodifica pixels sem copiar EXIF ou outros metadados de entrada. */
record Foto(byte[] bytes, String formato, String mimeType) {
    static final int MAX_BYTES = 5_000_000; // Convenção proposta: 5 MB decimais.
    static Foto validar(byte[] dados) {
        if (dados == null || dados.length == 0 || dados.length > MAX_BYTES)
            throw new IllegalArgumentException("Foto deve conter entre 1 byte e 5 MB.");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(dados))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Foto deve ser JPEG ou PNG válido.");
            var reader = readers.next();
            try {
                String formato = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png"))
                    throw new IllegalArgumentException("Foto deve ser JPEG ou PNG válido.");
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000)
                    throw new IllegalArgumentException("Foto excede 20 milhões de pixels.");
                var pixels = reader.read(0);
                var out = new ByteArrayOutputStream();
                if (!ImageIO.write(pixels, formato, out) || out.size() > MAX_BYTES)
                    throw new IllegalArgumentException("Foto recodificada excede 5 MB.");
                return new Foto(out.toByteArray(), formato.equals("jpeg") ? "jpg" : "png", "image/" + formato);
            } finally { reader.dispose(); }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a foto.");
        }
    }
}
