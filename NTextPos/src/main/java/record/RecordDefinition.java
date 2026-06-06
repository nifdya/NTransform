package record;

import java.util.ArrayList;
import java.util.List;

public class RecordDefinition {
    private String type;
    private int posType;
    private int lengthType;
    private List<Integer> longitudes = new ArrayList<>();
    private List<Boolean> ignorados = new ArrayList<>();

    // Constructor completo
    public RecordDefinition(String type, int posType, int lengthType) {
        this.type = type;
        this.posType = posType;
        this.lengthType = lengthType;
    }

    // Método para ir añadiendo los datos de cada campo <field>
    public void addField(int length, boolean ignore) {
        this.longitudes.add(length);
        this.ignorados.add(ignore);
    }

    // Métodos Getter explícitos para recuperar los valores en tu bucle principal
    public String getType() {
        return this.type;
    }

    public int getPosType() {
        return this.posType;
    }

    public int getLengthType() {
        return this.lengthType;
    }

    public List<Integer> getLongitudes() {
        return this.longitudes;
    }

    public List<Boolean> getIgnorados() {
        return this.ignorados;
    }
}

