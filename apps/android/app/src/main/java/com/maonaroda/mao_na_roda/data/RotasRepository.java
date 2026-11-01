package com.maonaroda.mao_na_roda.data;

import java.util.List;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;

public interface RotasRepository extends AutoCloseable {
    interface Cancelavel { void cancelar(); }
    interface Retorno<T> { void sucesso(T valor); void falha(String mensagem); }
    Cancelavel perfis(Retorno<List<Perfil>> retorno);
    Cancelavel buscar(String texto, Retorno<List<Destino>> retorno);
    Cancelavel planejar(Ponto origem, Ponto destino, String perfil, Retorno<Consulta> retorno);
    @Override void close();
}
