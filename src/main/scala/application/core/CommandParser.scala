package application.core

/**
 * Converts a string entry into an Enum [[CommandType]] for easier comparison.
 */
object CommandParser {

    /**
     * Converts a string entry into a [[CommandType]] enum for easier comparison. Returns the option [[CommandType.NO_ACTION]]
     * if a match cannot be found.
     *
     * @param command   the string to check
     * @return          a [[CommandType]]
     */
    def parse(command: String): CommandType = {
        if (command.equalsIgnoreCase("help")) {
            CommandType.HELP
        } else if (command.equalsIgnoreCase("exit")) {
            CommandType.EXIT
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
        } else {
            CommandType.NO_ACTION
        }
    }

    /**
     * [[CommandType CommandTypes]] often correspond one-to-one to [[Message Messages]] and serve to assist with translating
     * a string read by the command line into a message that is delivered to an [[akka.actor.Actor Actor]]
     *
     * @param name          a string name, the text input that corresponds to the command
     * @param description   a string description that is printed on info screens
     */
    enum CommandType(val name: String, val description: String) {
        case HELP       extends CommandType("help", "prints available commands")
        case EXIT       extends CommandType("exit", "terminates the program")
        case NEXT       extends CommandType("switch", "switches connection to the next available kiosk")
        case LIST       extends CommandType("list", "prints a list of all available events")
        case PURCHASE   extends CommandType("buy", "connects to a kiosk and purchases tickets")
        case ORDERS     extends CommandType("orders", "lists current orders")
        case SAVE       extends CommandType("save", "saves current orders to disk")
        case NO_ACTION  extends CommandType("none", "not a valid option")

        def getAll: List[CommandType] = {
            CommandType.values.toList
        }

    }

}
