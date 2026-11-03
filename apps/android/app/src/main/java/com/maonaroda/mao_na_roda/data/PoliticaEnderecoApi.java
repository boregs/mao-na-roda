package com.maonaroda.mao_na_roda.data;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class PoliticaEnderecoApi {
    private PoliticaEnderecoApi() { }
    public static boolean permitida(String url, boolean depuracao) {
        try {
            var uri = new URI(url);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) return false;
            if ("https".equalsIgnoreCase(uri.getScheme())) return true;
            return depuracao && "http".equalsIgnoreCase(uri.getScheme())
                    && Set.of("10.0.2.2", "127.0.0.1", "localhost").contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (Exception e) { return false; }
    }
}
