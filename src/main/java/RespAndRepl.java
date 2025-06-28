public class RespAndRepl {
    public final RespDataType resp;
    public final RespDataType repl;

    public RespAndRepl(RespDataType resp, RespDataType repl) {
        this.resp = resp;
        this.repl = repl;
    }

    public static RespAndRepl ofRepl(RespDataType resp) {
        return new RespAndRepl(resp, null);
    }
}
