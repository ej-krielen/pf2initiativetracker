package com.rekijan.initiativetrackersecondedition.core.data;

import static org.junit.Assert.assertNotNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A real single-fight save for the tests: a SharedPreferences file written on an emulator
 * (src/test/resources/shared_prefs_1_2_0.xml). Four characters, round 2: Valeros (first
 * in round, hero point), Seoni (Frightened 2 and persistent fire damage 1d6 DC 15), Kyra (dying 2,
 * wounded 1, the custom reaction Shield Block, used) and the Goblin Boss (not a PC).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class OneXSave {

    private OneXSave() {
    }

    /** Reads the prefs file into a store, the way SharedPreferences would. */
    public static MemoryKeyValueStore load() throws Exception {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        try (InputStream in = OneXSave.class.getResourceAsStream("/shared_prefs_1_2_0.xml")) {
            assertNotNull("Test save is missing", in);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            KeyValueStore.Editor editor = store.edit();
            NodeList strings = doc.getElementsByTagName("string");
            for (int i = 0; i < strings.getLength(); i++) {
                Element e = (Element) strings.item(i);
                editor.putString(e.getAttribute("name"), e.getTextContent());
            }
            NodeList ints = doc.getElementsByTagName("int");
            for (int i = 0; i < ints.getLength(); i++) {
                Element e = (Element) ints.item(i);
                editor.putInt(e.getAttribute("name"), Integer.parseInt(e.getAttribute("value")));
            }
            NodeList booleans = doc.getElementsByTagName("boolean");
            for (int i = 0; i < booleans.getLength(); i++) {
                Element e = (Element) booleans.item(i);
                editor.putBoolean(e.getAttribute("name"), Boolean.parseBoolean(e.getAttribute("value")));
            }
            editor.apply();
        }
        return store;
    }
}
