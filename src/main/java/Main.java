import java.util.Scanner;

public class Main {
    public static final String RESET = "\033[0m";
    public static final String EMPH = "\033[1;91m";

    public static void main(String[] args) {
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
            System.out.printf("%sWall %d Obstruction %d%s\n", EMPH, index + 1, i + 1, RESET);
            System.out.print("Please enter the width of the obstruction (metres): ");
            double obWidth = scanSysIn.nextDouble();

            System.out.print("Please enter the height of the obstruction (metres): ");
            double obHeight = scanSysIn.nextDouble();

            obstructionArea += obWidth * obHeight;
            System.out.println("========");
        }

        if (obstructionArea > baseArea) {
            System.err.println("There is no wall left!");
            System.exit(1);
        }

        return baseArea - obstructionArea;
    }
}
