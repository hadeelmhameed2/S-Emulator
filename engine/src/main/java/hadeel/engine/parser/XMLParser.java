package hadeel.engine.parser;

import hadeel.engine.model.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.util.*;
import org.xml.sax.InputSource;

public class XMLParser {
    
    public static ParseResult parseFile(String filePath) {
        ParseResult result = new ParseResult();
        
        if (!filePath.toLowerCase().endsWith(".xml")) {
            result.addError("File must have .xml extension");
            return result;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            result.addError("File does not exist: " + filePath);
            return result;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            
            Element root = doc.getDocumentElement();
            if (!"S-Program".equals(root.getNodeName())) {
                result.addError("Root element must be S-Program");
                return result;
            }
            
            SProgram program = new SProgram();
            
            String programName = root.getAttribute("name");
            if (programName == null || programName.trim().isEmpty()) {
                result.addError("Program must have a name attribute");
                return result;
            }
            program.setName(programName.trim());
            
            NodeList instructionsNodes = root.getElementsByTagName("S-Instructions");
            if (instructionsNodes.getLength() == 0) {
                result.addError("Program must contain S-Instructions element");
                return result;
            }
            
            Element instructionsElement = (Element) instructionsNodes.item(0);
            NodeList instructionList = instructionsElement.getElementsByTagName("S-Instruction");
            
            Set<String> definedLabels = new HashSet<>();
            Set<String> referencedLabels = new HashSet<>();
            
            for (int i = 0; i < instructionList.getLength(); i++) {
                Element instructionElement = (Element) instructionList.item(i);
                SInstruction instruction = parseInstruction(instructionElement, result);
                
                if (instruction != null) {
                    instruction.setLineNumber(i + 1);
                    program.addInstruction(instruction);
                    
                    if (instruction.getLabel() != null && !instruction.getLabel().isEmpty()) {
                        definedLabels.add(instruction.getLabel());
                    }
                    
                    collectReferencedLabels(instruction, referencedLabels);
                }
            }
            
            NodeList functionsNodes = root.getElementsByTagName("S-Functions");
            if (functionsNodes.getLength() > 0) {
                Element functionsElement = (Element) functionsNodes.item(0);
                NodeList functionList = functionsElement.getElementsByTagName("S-Function");
                
                for (int i = 0; i < functionList.getLength(); i++) {
                    Element functionElement = (Element) functionList.item(i);
                    SFunction function = parseFunction(functionElement, result);
                    if (function != null) {
                        program.addFunction(function);
                    }
                }
            }
            
            for (String label : referencedLabels) {
                if (!label.equals("EXIT") && !definedLabels.contains(label)) {
                    result.addError("Reference to undefined label: " + label);
                }
            }
            
            if (result.isSuccess()) {
                result.setProgram(program);
            }
            
        } catch (Exception e) {
            result.addError("Error parsing XML file: " + e.getMessage());
        }
        
        return result;
    }
    
    private static SInstruction parseInstruction(Element element, ParseResult result) {
        SInstruction instruction = new SInstruction();
        
        String typeStr = element.getAttribute("type");
        if (typeStr == null || typeStr.isEmpty()) {
            result.addError("Instruction must have type attribute");
            return null;
        }
        
        try {
            instruction.setType(InstructionType.fromString(typeStr));
        } catch (IllegalArgumentException e) {
            result.addError("Invalid instruction type: " + typeStr);
            return null;
        }
        
        String nameStr = element.getAttribute("name");
        if (nameStr == null || nameStr.isEmpty()) {
            result.addError("Instruction must have name attribute");
            return null;
        }
        
        try {
            instruction.setName(InstructionName.fromString(nameStr));
        } catch (IllegalArgumentException e) {
            result.addError("Invalid instruction name: " + nameStr);
            return null;
        }
        
        NodeList variableNodes = element.getElementsByTagName("S-Variable");
        if (variableNodes.getLength() > 0) {
            String variable = variableNodes.item(0).getTextContent().trim();
            instruction.setVariable(variable);
        }
        
        NodeList labelNodes = element.getElementsByTagName("S-Label");
        if (labelNodes.getLength() > 0) {
            String label = labelNodes.item(0).getTextContent().trim();
            instruction.setLabel(label);
        }
        
        NodeList argumentsNodes = element.getElementsByTagName("S-Instruction-Arguments");
        if (argumentsNodes.getLength() > 0) {
            Element argumentsElement = (Element) argumentsNodes.item(0);
            NodeList argumentList = argumentsElement.getElementsByTagName("S-Instruction-Argument");
            
            for (int i = 0; i < argumentList.getLength(); i++) {
                Element argElement = (Element) argumentList.item(i);
                String argName = argElement.getAttribute("name");
                String argValue = argElement.getAttribute("value");
                
                if (argName != null && argValue != null) {
                    instruction.addArgument(argName, argValue);
                }
            }
        }
        
        return instruction;
    }
    
    private static SFunction parseFunction(Element element, ParseResult result) {
        SFunction function = new SFunction();
        
        String name = element.getAttribute("name");
        if (name == null || name.isEmpty()) {
            result.addError("Function must have name attribute");
            return null;
        }
        function.setName(name);
        
        String userString = element.getAttribute("user-string");
        if (userString == null || userString.isEmpty()) {
            result.addError("Function must have user-string attribute");
            return null;
        }
        function.setUserString(userString);
        
        NodeList instructionsNodes = element.getElementsByTagName("S-Instructions");
        if (instructionsNodes.getLength() > 0) {
            Element instructionsElement = (Element) instructionsNodes.item(0);
            NodeList instructionList = instructionsElement.getElementsByTagName("S-Instruction");
            
            for (int i = 0; i < instructionList.getLength(); i++) {
                Element instructionElement = (Element) instructionList.item(i);
                SInstruction instruction = parseInstruction(instructionElement, result);
                if (instruction != null) {
                    function.addInstruction(instruction);
                }
            }
        }
        
        return function;
    }
    
    private static void collectReferencedLabels(SInstruction instruction, Set<String> labels) {
        String[] labelArgs = {"JNZLabel", "gotoLabel", "JZLabel",
                             "JEConstantLabel", "JEVariableLabel", "JEFunctionLabel"};

        for (String arg : labelArgs) {
            String label = instruction.getArgument(arg);
            if (label != null && !label.isEmpty()) {
                labels.add(label);
            }
        }
    }

    public static ParseResult parseXMLString(String xmlContent) {
        ParseResult result = new ParseResult();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlContent));
            Document doc = builder.parse(inputSource);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (!"S-Program".equals(root.getNodeName())) {
                result.addError("Root element must be S-Program");
                return result;
            }

            SProgram program = new SProgram();

            String programName = root.getAttribute("name");
            if (programName == null || programName.trim().isEmpty()) {
                result.addError("Program must have a name attribute");
                return result;
            }
            program.setName(programName.trim());

            NodeList instructionsNodes = root.getElementsByTagName("S-Instructions");
            if (instructionsNodes.getLength() == 0) {
                result.addError("Program must contain S-Instructions element");
                return result;
            }

            Element instructionsElement = (Element) instructionsNodes.item(0);
            NodeList instructionNodes = instructionsElement.getChildNodes();

            List<SInstruction> instructions = new ArrayList<>();
            System.out.println("[XMLParser] Found " + instructionNodes.getLength() + " instruction nodes");
            for (int i = 0; i < instructionNodes.getLength(); i++) {
                Node node = instructionNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element instElement = (Element) node;
                    SInstruction instruction = parseInstruction(instElement);
                    if (instruction != null) {
                        instructions.add(instruction);
                        System.out.println("[XMLParser] Parsed instruction: " + instruction.getName());
                    }
                }
            }

            program.setInstructions(instructions);
            System.out.println("[XMLParser] Total instructions parsed: " + instructions.size());

            // Parse functions if present
            NodeList functionsNodes = root.getElementsByTagName("S-Functions");
            System.out.println("[XMLParser] Found S-Functions nodes: " + functionsNodes.getLength());
            if (functionsNodes.getLength() > 0) {
                Element functionsElement = (Element) functionsNodes.item(0);
                NodeList functionNodes = functionsElement.getElementsByTagName("S-Function");
                System.out.println("[XMLParser] Found " + functionNodes.getLength() + " S-Function elements");

                for (int i = 0; i < functionNodes.getLength(); i++) {
                    Element funcElement = (Element) functionNodes.item(i);
                    SFunction function = parseFunction(funcElement);
                    if (function != null) {
                        program.addFunction(function);
                        System.out.println("[XMLParser] Parsed function: " + function.getName() + " with " + function.getInstructions().size() + " instructions");
                    }
                }
            }

            result.setProgram(program);
            System.out.println("[XMLParser] Final program has " + program.getInstructions().size() + " instructions and " + program.getFunctions().size() + " functions");

        } catch (Exception e) {
            result.addError("Error parsing XML: " + e.getMessage());
        }

        return result;
    }

    private static SFunction parseFunction(Element funcElement) {
        String name = funcElement.getAttribute("name");
        String userString = funcElement.getAttribute("user-string");
        System.out.println("[parseFunction] Parsing function: " + name);

        SFunction function = new SFunction(name, userString);

        NodeList instructionNodes = funcElement.getChildNodes();
        System.out.println("[parseFunction] Function " + name + " has " + instructionNodes.getLength() + " child nodes");

        int instructionCount = 0;
        for (int i = 0; i < instructionNodes.getLength(); i++) {
            Node node = instructionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element instElement = (Element) node;
                if ("S-Instructions".equals(instElement.getNodeName())) {
                    // Function has S-Instructions wrapper
                    System.out.println("[parseFunction] Found S-Instructions wrapper in function " + name);
                    NodeList funcInstructionNodes = instElement.getChildNodes();
                    for (int j = 0; j < funcInstructionNodes.getLength(); j++) {
                        Node funcNode = funcInstructionNodes.item(j);
                        if (funcNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element funcInstElement = (Element) funcNode;
                            SInstruction instruction = parseInstruction(funcInstElement);
                            if (instruction != null) {
                                function.addInstruction(instruction);
                                instructionCount++;
                            }
                        }
                    }
                } else {
                    SInstruction instruction = parseInstruction(instElement);
                    if (instruction != null) {
                        function.addInstruction(instruction);
                        instructionCount++;
                    }
                }
            }
        }

        System.out.println("[parseFunction] Function " + name + " parsed with " + instructionCount + " instructions");
        return function;
    }

    private static SInstruction parseInstruction(Element element) {
        SInstruction instruction = new SInstruction();

        String typeStr = element.getAttribute("type");
        if (typeStr == null || typeStr.isEmpty()) {
            System.out.println("[parseInstruction] ERROR: Instruction missing type attribute");
            return null;
        }

        try {
            instruction.setType(InstructionType.fromString(typeStr));
        } catch (IllegalArgumentException e) {
            System.out.println("[parseInstruction] ERROR: Invalid instruction type: " + typeStr);
            return null;
        }

        String nameStr = element.getAttribute("name");
        if (nameStr == null || nameStr.isEmpty()) {
            System.out.println("[parseInstruction] ERROR: Instruction missing name attribute");
            return null;
        }

        try {
            instruction.setName(InstructionName.fromString(nameStr));
        } catch (IllegalArgumentException e) {
            System.out.println("[parseInstruction] ERROR: Invalid instruction name: " + nameStr);
            return null;
        }

        NodeList variableNodes = element.getElementsByTagName("S-Variable");
        if (variableNodes.getLength() > 0) {
            String variable = variableNodes.item(0).getTextContent().trim();
            instruction.setVariable(variable);
        }

        NodeList labelNodes = element.getElementsByTagName("S-Label");
        if (labelNodes.getLength() > 0) {
            String label = labelNodes.item(0).getTextContent().trim();
            instruction.setLabel(label);
        }

        NodeList argumentsNodes = element.getElementsByTagName("S-Instruction-Arguments");
        if (argumentsNodes.getLength() > 0) {
            Element argumentsElement = (Element) argumentsNodes.item(0);
            NodeList argumentList = argumentsElement.getElementsByTagName("S-Instruction-Argument");

            for (int i = 0; i < argumentList.getLength(); i++) {
                Element argElement = (Element) argumentList.item(i);
                String argName = argElement.getAttribute("name");
                String argValue = argElement.getAttribute("value");

                if (argName != null && argValue != null) {
                    instruction.addArgument(argName, argValue);
                }
            }
        }

        return instruction;
    }
}