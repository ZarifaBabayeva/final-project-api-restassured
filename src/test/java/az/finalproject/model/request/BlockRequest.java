package az.finalproject.model.request;

/** POST /cards/{id}/block body. reason: TEMPORARY | LOST | STOLEN */
public class BlockRequest {
    public String reason;

    public BlockRequest(String reason) {
        this.reason = reason;
    }
}
