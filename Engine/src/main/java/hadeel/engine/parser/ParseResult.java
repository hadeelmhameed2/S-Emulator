package hadeel.engine.parser;

import hadeel.engine.model.SProgram;
import java.util.ArrayList;
import java.util.List;

public class ParseResult {
    private SProgram program;
    private List<String> errors;
    
    public ParseResult() {
        this.errors = new ArrayList<>();
    }
    
    public boolean isSuccess() {
        return errors.isEmpty();
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public SProgram getProgram() {
        return program;
    }
    
    public void setProgram(SProgram program) {
        this.program = program;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        return String.join("\n", errors);
    }
}