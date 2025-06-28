public class RespAndRepl {
    public final RespDataType resp;
    public final RespDataType repl;
    public final boolean isPropagatedToReplica;

    public RespAndRepl(RespDataType resp, RespDataType repl, boolean isPropagatedToReplica) {
        this.resp = resp;
        this.repl = repl;
        this.isPropagatedToReplica = isPropagatedToReplica;
    }

    public static RespAndRepl ofRespForWrite(RespDataType resp) {
        return new RespAndRepl(resp, null, true);
    }

    public static RespAndRepl ofRespForRead(RespDataType resp) {
        return new RespAndRepl(resp, null, false);
    }
}
