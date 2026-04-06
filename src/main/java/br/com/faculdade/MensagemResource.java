package br.com.faculdade;

import jakarta.ws.rs.*; import jakarta.ws.rs.core.MediaType; import jakarta.ws.rs.core.Response; import java.time.LocalDateTime; import java.util.ArrayList; import java.util.List; import java.util.Optional; import java.util.concurrent.atomic.AtomicLong;

@Path("/mensagens") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) public class MensagemResource {

    // Armazenamento em memória conforme exigido
    private final List<Mensagem> repositorio = new ArrayList<>();
    private final AtomicLong geradorId = new AtomicLong(1);

    @GET
    public Response listarTodas() {
        return Response.ok(repositorio).build(); // Retorna 200 OK
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Optional<Mensagem> mensagem = repositorio.stream()
                .filter(m -> m.id.equals(id))
                .findFirst();

        if (mensagem.isPresent()) {
            return Response.ok(mensagem.get()).build(); // Retorna 200 OK
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // Retorna 404 Not Found
    }

    @POST
    public Response enviarMensagem(Mensagem novaMensagem) {
        novaMensagem.id = geradorId.getAndIncrement();
        novaMensagem.timestamp = LocalDateTime.now();
        repositorio.add(novaMensagem);

        return Response.status(Response.Status.CREATED) // Retorna 201 Created
                .entity(novaMensagem)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response removerMensagem(@PathParam("id") Long id) {
        boolean removido = repositorio.removeIf(m -> m.id.equals(id));

        if (removido) {
            return Response.noContent().build(); // Retorna 204 No Content (sucesso sem corpo)
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // Retorna 404 se não achar
    }

}