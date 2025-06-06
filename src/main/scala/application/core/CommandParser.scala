package application.core

/**
 * Converts a string entry into an Enum [[CommandType]] for easier comparison.
 */
object CommandParser {

    /**
     * Converts a string entry into an Enum [[CommandType]] for easier comparison. Returns the option [[CommandType.NO_ACTION]]
     * if a match cannot be found.
     *
     * @param command   the string to check
     * @return          a [[CommandType]]
     */
    def parse(command: String): CommandType = {
        if (command.equalsIgnoreCase("exit")) {
            CommandType.EXIT
        } else if (command.equalsIgnoreCase("report")) {
            CommandType.REPORT
        } else if (command.equalsIgnoreCase("switch")) {
            CommandType.NEXT
        } else if (command.equalsIgnoreCase("list")) {
            CommandType.LIST
        } else if (command.equalsIgnoreCase("buy")) {
            CommandType.PURCHASE
        } else if (command.equalsIgnoreCase("orders")) {
            CommandType.ORDERS
        } else if (command.equalsIgnoreCase("save")) {
            CommandType.SAVE
        } else if (command.equalsIgnoreCase("message")) {
            CommandType.MESSAGE
        } else if (command.equalsIgnoreCase("kill")) {
            CommandType.KILL
        } else {
            CommandType.NO_ACTION
        }
    }

    enum CommandType(val name: String, val description: String) {
        case EXIT       extends CommandType("exit", "terminates the program")
        case REPORT     extends CommandType("report", "queries kiosks for their status")
        case NEXT       extends CommandType("switch", "switches connection to the next available kiosk")
        case LIST       extends CommandType("list", "prints a list of all available events")
        case PURCHASE   extends CommandType("buy", "connects to a kiosk and purchases tickets")
        case ORDERS     extends CommandType("orders", "lists current orders")
        case SAVE       extends CommandType("save", "saves current orders to disk")
        case MESSAGE    extends CommandType("message", "sends a string message to a kiosk")
        case RING       extends CommandType("start", "starts the token ring")
        case KILL       extends CommandType("kill", "forces a shutdown of a kiosk")
        case NO_ACTION  extends CommandType("none", "not a valid option")

        def getAll: List[CommandType] = {
            CommandType.values.toList
        }
    }

}
