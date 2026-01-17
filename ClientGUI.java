// this is the entry point for the gui version of the client, which uses swing components
// its only job is to act as a starting point and forward to the gameclientgui class
// this allows us to have different entry points for console and gui modes
// the actual gui implementation logic is handled in gameclientgui.java file
public class ClientGUI {
    public static void main(String[] args) {
        // this line starts the gui client by calling gameclientgui's main method
        // the gui version provides a more user-friendly interface compared to console
        GameClientGUI.main(args);
    }
}




