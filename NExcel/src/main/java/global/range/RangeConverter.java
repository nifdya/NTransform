package global.range;

import picocli.CommandLine.ITypeConverter;
import java.util.ArrayList;
import java.util.List;


/**
 * Clase para la conversión del listado de parámetros
 * Procesa la cadena y obtiene el listado de  {@link ParamRange}   
 */
public class RangeConverter implements ITypeConverter<List<ParamRange>> {
    @Override
    public List<ParamRange> convert(String value) throws Exception {
        List<ParamRange> result = new ArrayList<>();
        // Separamos por #
        String[] blocks = value.split("#");
        
        for (String block : blocks) {
            // Separamos por -
            String[] partes = block.split("-");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato incorrecto en: " + block);
            }
            int start = Integer.parseInt(partes[0]);
            int end = Integer.parseInt(partes[1]);
            result.add(new ParamRange(start, end));
        }
        return result;
    }
}
