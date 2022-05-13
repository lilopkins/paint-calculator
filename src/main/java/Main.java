import java.util.Scanner;

/**
 * A calculator to calculate the amount of paint needed to paint walls.
 */
public class Main {
    /**
     * The ANSI code to reset any terminal formatting.
     */
    public static String RESET = "\033[0m";

    /**
     * The ANSI code to format the terminal as bold, red text.
     */
    public static String EMPH = "\033[1;91m";

    public static void main(String[] args) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            EMPH = RESET = "";
        }

        Scanner scanSysIn = new Scanner(System.in);

        System.out.print("How many walls are there?: ");
        int numWalls = scanSysIn.nextInt();

        double totalArea = 0;
        for (int i = 0; i < numWalls; i++) {
            totalArea += askWall(scanSysIn, i);
            System.out.println("================");
        }

        // How many coats of paint?
        System.out.print("Please enter how many coats of paint you want: ");
        int coats = scanSysIn.nextInt();

        final float litresPerSqM = 4;

        double paintQty = litresPerSqM * coats * totalArea;

        // Paint can size
        System.out.print("Please enter how big your paint cans are (litres): ");
        double canSize = scanSysIn.nextDouble();
        System.out.println("================");

        double numberCansExact = paintQty / canSize;
        int numberCans = (int) Math.ceil(numberCansExact);
        String pluralChar = numberCans == 1 ? "" : "s";

        System.out.printf("You will need %d can%s (%.2f litres)!\n", numberCans, pluralChar, paintQty);
    }

    /**
     * Query the user for details to calculate the area of the wall.
     * @param scanSysIn The scanner instance connected to the user's input, probably sysin.
     * @param index The zero-based index of the wall.
     * @return The area of the wall.
     */
    private static double askWall(Scanner scanSysIn, int index) {
        System.out.printf("%sWall %d%s\n", EMPH, index + 1, RESET);
        // Enter wall width and height
        System.out.print("Please enter the width of the wall (metres): ");
        double width = scanSysIn.nextDouble();

        System.out.print("Please enter the height of the wall (metres): ");
        double height = scanSysIn.nextDouble();

        double baseArea = width * height;

        // Enter potentially infinite obstructions and width and height
        double obstructionArea = 0;

        System.out.println("========");
        System.out.print("How many obstructions are there?: ");
        int numObstructions = scanSysIn.nextInt();

        for (int i = 0; i < numObstructions; i++) {
            obstructionArea += askObstruction(scanSysIn, index, i);
            System.out.println("========");
        }

        if (obstructionArea > baseArea) {
            System.err.println("There is no wall left!");
            System.exit(1);
        }

        return baseArea - obstructionArea;
    }

    /**
     * Query the user for details about an obstruction to painting the wall.
     * @param scanSysIn The scanner instance connected to the user's input, probably sysin.
     * @param wallIndex The zero-based index of the wall this obstruction is located on.
     * @param index The zero-based index of this obstruction on the wall.
     * @return The area of the obstruction.
     */
    private static double askObstruction(Scanner scanSysIn, int wallIndex, int index) {
        System.out.printf("%sWall %d Obstruction %d%s\n", EMPH, wallIndex + 1, index + 1, RESET);

        System.out.println("What is the shape of the obstruction? 1 - rectangular (or square), 2 - circular, 3 - oval");
        int option = scanSysIn.nextInt();
        return switch (option) {
            case 1: // rectangular
                System.out.print("Please enter the width of the obstruction (metres): ");
                double obWidth = scanSysIn.nextDouble();
                System.out.print("Please enter the height of the obstruction (metres): ");
                double obHeight = scanSysIn.nextDouble();
                yield obWidth * obHeight;

            case 2: // circular
                System.out.print("Please enter the radius of the obstruction (metres): ");
                double obRadius = scanSysIn.nextDouble();
                yield 2.d * Math.PI * obRadius;

            case 3: // oval
                System.out.print("Please enter the length of the semi-major axis of the obstruction (metres): ");
                double obSemiMajor = scanSysIn.nextDouble();
                System.out.print("Please enter the length of the semi-minor axis of the obstruction (metres): ");
                double obSemiMinor = scanSysIn.nextDouble();
                yield Math.PI * obSemiMajor * obSemiMinor;

            default:
                System.out.println("Nonexistent shape...");
                yield 0;
        };
    }
}
