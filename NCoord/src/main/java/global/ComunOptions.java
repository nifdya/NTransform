package global;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunOptions {
    public String inputFile;
    public String outputFile;
    public String tempFile; // Estructura base (opcional para logs)
    public List<Command> commands;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Command {
    public String jar;
    public String step;
    public String cmdOptions;
    public Boolean keepOnError;
    public List<SubTarea> tasks;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SubTarea {
    public String task; // Ejemplo: "nomtarea1|p1=v1|p2=v2"

    public String getNombreLimpio() {
        if (task == null) return "Sin_Nombre";
        return task.split("\\|")[0];
    }
    @Override
    public String toString() {
    	return this.task;
    }
}
