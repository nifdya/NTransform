package global;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunOptions {
    public String ficheroInicial;
    public String ficheroFinal;
    public String ficherosIntermedios; // Estructura base (opcional para logs)
    public List<Instruccion> instrucciones;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Instruccion {
    public String jar;
    public String paso;
    public Boolean manteneSiError;
    public List<SubTarea> tareas;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SubTarea {
    public String tarea; // Ejemplo: "nomtarea1|p1=v1|p2=v2"

    public String getNombreLimpio() {
        if (tarea == null) return "Sin_Nombre";
        return tarea.split("\\|")[0];
    }
    @Override
    public String toString() {
    	return this.tarea;
    }
}
