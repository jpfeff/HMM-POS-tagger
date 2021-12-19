import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Part of speech tagging using a Hidden Markov Model and Viterbi algorithm
 *
 * @author Josh Pfefferkorn
 * CS10, Fall 2020
 */

public class Viterbi {

    // list of words and tags parsed from a training file
    public ArrayList<String> words;
    public ArrayList<String> tags;
    // maps words to parts of speech and the words likelihood of being that part of speech
    public Map<String,Map<String,Double>> wordToPOS;
    // maps parts of speech to the other parts of speech and the likelihood that the second part of speech follows the first
    public Map<String,Map<String,Double>> POStoTransition;
    // unseen word penalty
    public final int UNOBSERVED = -30;

    /**
     * Constructor that takes training files and uses static methods to create
     * observation and transition probability maps
     */
    public Viterbi(String wordsFileName, String tagsFileName) throws IOException {
        words = getWordsOrTags(wordsFileName, false);
        tags = getWordsOrTags(tagsFileName, true);
        wordToPOS = normalizeWordToPOS(mapWordsToPOS(words,tags),tags);
        POStoTransition = normalizePOSToTransition(mapPOStoTransition(tags));
    }

    /**
     * Parses training files into list of either words or parts of speech depending on the file type
     * @param tags: if true, the training file contains parts of speech and is therefore not made lowercase
     *            if false, the training file contains sentences and is made lowercase
     */
    public static ArrayList<String> getWordsOrTags(String fileName, boolean tags) throws IOException {
        // declare reader
        BufferedReader input;
        // instantiate list for words or tags
        ArrayList<String> result = new ArrayList<>();
        // try creating a reader for the file
        try {
            input = new BufferedReader(new FileReader(fileName));
        }
        // catch if the file doesn't exist
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            // return empty list
            return result;
        }
        try {
            String line;
            // read input line by line
            while ((line = input.readLine()) != null) {
                // lowercase only if file contains sentences
                if (!tags) {
                    line = line.toLowerCase();
                }
                // in front of each line, add a # for start
                result.add("#");
                // split it into words or tags
                String[] pieces = line.split(" ");
                // add these words or tags to the list
                for (int k=0; k< pieces.length; k++) {
                    result.add(pieces[k]);
                }
            }
        }
        // if error while reading, catch it
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }
        input.close();
        // return completed list
        return result;
    }

    /**
     * Maps words to parts of speech and the number of times that they appear as that part of speech
     */
    public static Map<String, Map<String,Double>> mapWordsToPOS(ArrayList<String> words, ArrayList<String> tags) {
        Map<String, Map<String,Double>> wordToPOS = new HashMap<>();
        // loop through words (and tags; they should be the same size)
        for (int k = 0; k< words.size(); k++) {
            // store current word and tag
            String curWord = words.get(k);
            String curTag = tags.get(k);
            // if the word doesn't already exist in the map
            if (!wordToPOS.containsKey(curWord)) {
                // add it and its part of speech (with a frequency of 1)
                wordToPOS.put(curWord, new HashMap<String,Double>());
                wordToPOS.get(curWord).put(curTag,1.0);
            }
            // if the word exists, but the tag doesn't
            else if (!wordToPOS.get(curWord).containsKey(curTag)) {
                // add the tag to the word's map with a frequency of 1
                wordToPOS.get(curWord).put(curTag,1.0);
            }
            else {
                // increment tag frequency
                wordToPOS.get(curWord).put(curTag,wordToPOS.get(curWord).get(curTag)+1);
            }
        }
        // take out the start markers, these are only needed for the transitions map
        wordToPOS.remove("#");
        return wordToPOS;
    }
    /**
     * Normalizes map created by previous method by dividing individual frequencies by total frequency of that part of speech
     */
    public static Map<String, Map<String,Double>> normalizeWordToPOS(Map<String, Map<String,Double>> wordToPOS, ArrayList<String> tags) {
        Map<String,Double> POStoFrequency = new HashMap<>();
        // loop over tags
        for (String tag : tags) {
            // if the frequency map doesn't already contain it
            if (!POStoFrequency.containsKey(tag)) {
                // insert it and set frequency of the tag to 1
                POStoFrequency.put(tag,1.0);
            }
            else {
                // otherwise increment frequency
                POStoFrequency.put(tag,POStoFrequency.get(tag)+1);
            }
        }
        // loop over words map created in previous method
        for (String curWord : wordToPOS.keySet()) {
            // loop over tags for each word
            for (String curTag : wordToPOS.get(curWord).keySet()) {
                // normalize by dividing each frequency by total frequency
                wordToPOS.get(curWord).put(curTag, Math.log(wordToPOS.get(curWord).get(curTag) / POStoFrequency.get(curTag)));
            }
        }
        // return normalized map
        return wordToPOS;
    }
    /**
     * Maps parts of speech to other parts of speech and the number of time a transition from the first
     * to the second occurs
     */
    public static Map<String, Map<String,Double>> mapPOStoTransition(ArrayList<String> tags) {
        Map<String, Map<String, Double>> POStoTransition = new HashMap<>();
        // loop over tags
        for (int k = 0; k < tags.size() - 1; k++) {
            // store each tag and the tag after it
            String curTag = tags.get(k);
            String nextTag = tags.get(k + 1);
            // if the first tag isn't already in the map
            if (!POStoTransition.containsKey(curTag)) {
                // add the tag and the second tag with a value of 1 as the first tag's value
                POStoTransition.put(curTag, new HashMap<String,Double>());
                POStoTransition.get(curTag).put(nextTag,1.0);
            }
            // if the first tag is in the map, but the second isn't
            else if (!POStoTransition.get(curTag).containsKey(nextTag)) {
                // add the second tag with a value of 1
                POStoTransition.get(curTag).put(nextTag,1.0);
            }
            else {
                // otherwise increment second tag frequency
                POStoTransition.get(curTag).put(nextTag,POStoTransition.get(curTag).get(nextTag)+1);
            }
        }
        // there should not be a transition at the end of the sentence
        POStoTransition.remove(".");
        // return completed map
        return POStoTransition;
    }
    /**
     * Normalizes map created by previous method by dividing individual transition frequencies by the total number
     * of transitions from the first tag
     */
    public static Map<String, Map<String,Double>> normalizePOSToTransition(Map<String, Map<String,Double>> POStoTransition) {
        // loop over the tags
        for (String curTag: POStoTransition.keySet()) {
            // set initial transition frequency for the first tag to 0
            Double totalFreq = 0.0;
            // loop over tags that come after first tag
            for (String nextTag: POStoTransition.get(curTag).keySet()) {
                // add each of their frequencies to the total
                totalFreq += POStoTransition.get(curTag).get(nextTag);
            }
            // for each second tag, normalize frequency by dividing by total number of transitions from first tag
            for (String nextTag: POStoTransition.get(curTag).keySet()) {
                POStoTransition.get(curTag).put(nextTag,Math.log(POStoTransition.get(curTag).get(nextTag)/totalFreq));
            }
        }
        // return normalized map
        return POStoTransition;
    }
    /**
     * Takes a text file and returns an list of guessed parts of speech sequentially
     */
    public ArrayList<String> fileTagger(String fileName) throws IOException {
        // declare reader
        BufferedReader input = null;
        // initialize final list
        ArrayList<String> allTags = new ArrayList();
        // try creating a reader for the file
        try {
            input = new BufferedReader(new FileReader(fileName));
        }
        // catch if the file doesn't exist
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
        }
        try {
            String line;
            // read input line by line
            while ((line = input.readLine()) != null) {
                // create an arraylist with the current possible states, adding the start state #
                ArrayList<String> curStates = new ArrayList();
                curStates.add("#");
                // create a map that maps from states to scores, adding the start state # and its score 0
                Map<String,Double> curScores = new HashMap<>();
                curScores.put("#", 0.0);
                // initialize a map that will allow tracing back along the best possible path of tags
                ArrayList<Map<String,String>> backTrack = new ArrayList<>();
                // create an arraylist to hold potential next states
                ArrayList nextStates = new ArrayList();
                // make line lowercase
                line = line.toLowerCase();
                // split line up by spaces
                String[] pieces = line.split(" ");
                // loop over words
                for (int i = 0; i<pieces.length; i++) {
                    // create a numbered map entry for each word
                    backTrack.add(i, new HashMap<>());
                    // initialize map for next scores
                    HashMap<String, Double> nextScores = new HashMap<>();
                    // loop over current possible states
                    for (String curState : curStates) {
                        // initialize arraylist for potential next states
                        nextStates = new ArrayList();
                        // if part of speech has been seen before
                        if (POStoTransition.containsKey(curState)) {
                            // loop over possible transitions
                            for (String nextState : POStoTransition.get(curState).keySet()) {
                                // add each possible next state to next states
                                nextStates.add(nextState);
                                // declare a double for the score of each possible next state
                                double nextScore;
                                // calculate the score by adding the score of the current state, the transition score, and the observation score
                                if (!wordToPOS.containsKey(pieces[i]) || !wordToPOS.get(pieces[i]).containsKey(nextState)) {
                                    // used final variable as observation score if word is new
                                    nextScore = curScores.get(curState) + POStoTransition.get(curState).get(nextState) + UNOBSERVED;
                                } else {
                                    nextScore = curScores.get(curState) + POStoTransition.get(curState).get(nextState) + wordToPOS.get(pieces[i]).get(nextState);
                                }
                                // if next scores doesn't have any scores for the next state or if the score is better than previous scores
                                if (!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)) {
                                    // add it to next scores
                                    nextScores.put(nextState, nextScore);
                                    // add the current and previous state to backtrack to remember how we got there
                                    backTrack.get(i).put(nextState, curState);
                                }
                            }
                        }
                    }
                    // make potential next states and scores the current possible states and scores
                    curStates = nextStates;
                    curScores = nextScores;
                }
                //
                ArrayList<String> result = new ArrayList();
                // initialize current tag to empty
                String tag = "";
                // make the best score worse than it ever will be for any state
                Double score = -1000.0;
                // loop over scores for current tag
                for (String curTag: curScores.keySet()) {
                    // if the tags score is better than the previous best score
                    if(curScores.get(curTag) > score) {
                        // update the score
                        score = curScores.get(curTag);
                        // and the tag
                        tag = curTag;
                    }
                }
                // store that tag as the previous one
                String prevTag = tag;
                // loop over backtrack, starting at the end
                for (int k = backTrack.size()-1; k>=0; k--) {
                    // add the current tag to the front of the result list
                    result.add(0,tag);
                    // add the tag that got us to that tag
                    prevTag = backTrack.get(k).get(tag);
                    // work backwards, updating tag to previous
                    tag = prevTag;
                }
                // add each line of tags to the final list of tags
                for (String s: result) {
                    allTags.add(s);
                }
            }
        }
        // if error while reading, catch it
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }
        input.close();
        // return completed array
        return allTags;
    }
    /**
     * Tests the accuracy of the fileTagger method by comparing guessed tags to actual tags
     */
    public double testAccuracy(ArrayList<String> foundTags, ArrayList<String> realTags) {
        // initialize doubles for total tags and correct ones
        double totalTags = 0.0;
        double correctTags = 0.0;
        // strip out # from the tags list
        for (int k = 0; k<realTags.size(); k++) {
            if (realTags.get(k).equals("#")) {
                realTags.remove(k);
            }
        }
        // loop over real tags (and guessed tags, they should be the same size)
        for (int k =0; k<realTags.size();k++) {
            // increment total tags
            totalTags++;
            // if the tags match
            if(foundTags.get(k).equals(realTags.get(k))) {
                // increment correct tags
                correctTags++;
            }
        }
        // print results
        System.out.println("The tagger got a total of "+correctTags+" tags correct out of " + totalTags + " total, with an accuracy of " + correctTags/totalTags*100 + "%");
        // return accuracy
        return correctTags/totalTags;
    }
    /**
     * Essentially performs the same as fileTagger(), but takes console input rather than files
     */
    public void inputTagger() {
        // prompt user to enter a sentence
        System.out.println("Please enter a sentence to have its parts of speech guessed");
        // declare scanner
        Scanner in = new Scanner(System.in);
        // read input by line
        String line = in.nextLine();
        // while there is still input to read
        while (line != null) {
            // create an arraylist with the current possible states, adding the start state #
            ArrayList<String> curStates = new ArrayList();
            curStates.add("#");
            // create a map that maps from states to scores, adding the start state # and its score 0
            Map<String, Double> curScores = new HashMap<>();
            curScores.put("#", 0.0);
            // initialize a map that will allow tracing back along the best possible path of tags
            ArrayList<Map<String, String>> backTrack = new ArrayList<>();
            // create an arraylist to hold potential next states
            ArrayList<String> nextStates = new ArrayList<>();
            // split line up by spaces
            String[] pieces = line.split(" ");
            // loop over words
            for (int i = 0; i < pieces.length; i++) {
                // create a numbered map entry for each word
                backTrack.add(i, new HashMap<>());
                // initialize map for next scores
                HashMap<String, Double> nextScores = new HashMap<>();
                // loop over current possible states
                for (String curState : curStates) {
                    // initialize arraylist for potential next states
                    nextStates = new ArrayList();
                    // if part of speech has been seen before
                    if (POStoTransition.containsKey(curState)) {
                        // loop over possible transitions
                        for (String nextState : POStoTransition.get(curState).keySet()) {
                            // add each possible next state to next states
                            nextStates.add(nextState);
                            // declare a double for the score of each possible next state
                            double nextScore;
                            // calculate the score by adding the score of the current state, the transition score, and the observation score
                            if (!wordToPOS.containsKey(pieces[i]) || !wordToPOS.get(pieces[i]).containsKey(nextState)) {
                                // used final variable as observation score if word is new
                                nextScore = curScores.get(curState) + POStoTransition.get(curState).get(nextState) + UNOBSERVED;
                            } else {
                                nextScore = curScores.get(curState) + POStoTransition.get(curState).get(nextState) + wordToPOS.get(pieces[i]).get(nextState);
                            }
                            // if next scores doesn't have any scores for the next state or if the score is better than previous scores
                            if (!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)) {
                                // add it to next scores
                                nextScores.put(nextState, nextScore);
                                // add the current and previous state to backtrack to remember how we got there
                                backTrack.get(i).put(nextState,curState);
                            }
                        }
                    }
                }
                // make potential next states and scores the current possible states and scores
                curStates = nextStates;
                curScores = nextScores;
            }
            // initialize current tag to empty
            ArrayList<String> result = new ArrayList();
            String tag = "";
            // make the best score worse than it ever will be for any state
            Double score = -1000.0;
            // loop over scores for current tag
            for (String curTag : curScores.keySet()) {
                // if the tags score is better than the previous best score
                if (curScores.get(curTag) > score) {
                    // update the score
                    score = curScores.get(curTag);
                    // and the tag
                    tag = curTag;
                }
            }
            // store that tag as the previous one
            String prevTag = tag;
            // loop over backtrack, starting at the end
            for (int k = backTrack.size() - 1; k >= 0; k--) {
                // add the current tag to the front of the result list
                result.add(0, tag);
                // add the tag that got us to that tag
                prevTag = backTrack.get(k).get(tag);
                // work backwards, updating tag to previous
                tag = prevTag;
            }
            // print out tags
            System.out.println(result);
            // update line to the next input the user gives
            line = in.nextLine();
        }
    }

    public static void main(String[] args) throws IOException {
        Viterbi v = new Viterbi("inputs/brown-train-sentences.txt","inputs/brown-train-tags.txt");
        ArrayList foundTags = v.fileTagger("inputs/brown-test-sentences.txt");
        ArrayList realTags = getWordsOrTags("inputs/brown-test-tags.txt", true);
        v.testAccuracy(foundTags,realTags);
        v.inputTagger();
    }
}
