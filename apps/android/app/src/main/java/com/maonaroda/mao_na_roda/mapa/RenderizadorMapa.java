package com.maonaroda.mao_na_roda.mapa;

import android.content.Context;
import android.content.res.Configuration;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.maonaroda.mao_na_roda.R;
import com.maonaroda.mao_na_roda.data.DadosRotas.Rota;
import java.util.*;

public final class RenderizadorMapa {
    private final GoogleMap mapa;
    private Marker posicao;
    private List<MarkerOptions> barreirasLocais = List.of();
    public void posicao(com.maonaroda.mao_na_roda.data.DadosRotas.Ponto ponto) {
        if (ponto == null) {
            if (posicao != null) posicao.remove();
            posicao = null;
            return;
        }
        var coordenada = new LatLng(ponto.latitude(), ponto.longitude());
        if (posicao == null) posicao = mapa.addMarker(new MarkerOptions().position(coordenada)
                .title("Sua posição").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        else posicao.setPosition(coordenada);
    }

    public void recentralizar() {
        if (posicao != null) mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicao.getPosition(), 17));
    }

    private String assinatura = "";
    public RenderizadorMapa(GoogleMap mapa, Context context) {
        this.mapa = mapa;
        mapa.clear();
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setCompassEnabled(true);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.55, -46.63), 12));
        if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            mapa.setMapStyle(new MapStyleOptions("[{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#14252c\"}]},{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#ccd0cf\"}]},{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"#06141b\"}]}]"));
    }
    public void barreirasLocais(List<MarkerOptions> marcadores) {
        barreirasLocais = marcadores;
        barreirasLocais.forEach(mapa::addMarker);
    }

    public void mostrarPonto(LatLng ponto) {
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(ponto, 17));
    }

    public void padding(int bottom) { mapa.setPadding(12, 12, 12, Math.max(bottom, 12)); }
    public void renderizar(List<Rota> rotas, int selecionada, Context context, int width, int height) {
        if (width <= 0 || height <= 0) return;
        String id = rotas.isEmpty() ? "" : rotas.stream().map(Rota::id).collect(java.util.stream.Collectors.joining(",")) + ":" + selecionada;
        if (id.equals(assinatura)) return;
        assinatura = id;
        mapa.clear();
        posicao = null;
        barreirasLocais.forEach(mapa::addMarker);
        if (rotas.isEmpty()) return;
        var bounds = new LatLngBounds.Builder();
        for (int i = 0; i < rotas.size(); i++) {
            var polyline = new PolylineOptions().width(i == selecionada ? 12 : 6)
                    .color(ContextCompat.getColor(context, i == selecionada ? R.color.ocean : R.color.outline))
                    .zIndex(i == selecionada ? 2 : 1);
            for (var p : rotas.get(i).pontos()) {
                var latLng = new LatLng(p.latitude(), p.longitude()); polyline.add(latLng);
                if (i == selecionada) bounds.include(latLng);
            }
            mapa.addPolyline(polyline);
        }
        var points = rotas.get(selecionada).pontos();
        mapa.addMarker(new MarkerOptions().position(new LatLng(points.get(0).latitude(), points.get(0).longitude())).title("Origem"));
        var fim = points.get(points.size() - 1);
        mapa.addMarker(new MarkerOptions().position(new LatLng(fim.latitude(), fim.longitude())).title("Destino"));
        if (width > 0 && height > 0) mapa.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), width, height, 48));
    }
}
