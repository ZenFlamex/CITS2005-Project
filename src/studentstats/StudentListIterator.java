package studentstats;

import java.util.NoSuchElementException;
import itertools.DoubleEndedIterator;
import studentapi.*;

/**
 * A (double ended) iterator over student records pulled from the student API.
 *
 * <p>This does not load the whole student list immediately, but rather queries the API ({@link
 * StudentList#getPage}) only as needed.
 */
public class StudentListIterator implements DoubleEndedIterator<Student> {
    
    private final StudentList list; // List of students
    private final int maxRetries; // Maximum number of retries
    private int currentPageIndex; // Index of the current page
    private Student[] currentPage; // Current page of students
    private int start; // Index of the first student in the current page only for the front iteration
    private int end; // Index of the last student in the current page only for the front iteration
    private int reversePageIndex; // Index of the current student in the reverse iteration only for the back iteration
    private int reversePageNumber; // Index of the current page in the reverse iteration
    private int remainingStudents; // Number of students remaining to iterate 

    /**
     * Construct an iterator over the given {@link StudentList} with the specified retry quota.
     *
     * @param list The API interface.
     * @param retries The number of times to retry a query after getting {@link
     *     QueryTimedOutException} before declaring the API unreachable and throwing an {@link
     *     ApiUnreachableException}.
     */
    public StudentListIterator(StudentList list, int retries) {
        
        // Initialize the fields
        this.list = list;
        this.maxRetries = retries;
        this.currentPageIndex = -1; // Initialize to -1 to indicate no page loaded yet, lazy loading
        this.currentPage = null; // No page loaded initially
        this.start = 0; // Will be set when the first page is loaded
        this.end = -1 ; // Will be set when the first page is loaded
        this.reversePageIndex = -1; // Initialized to -1 to indicate no page loaded yet, lazy loading
        this.reversePageNumber = list.getNumPages() - 1; // Start from the last page -1 because it is 0-indexed
        this.remainingStudents = list.getNumStudents(); // Connects next() and reverseNext() together
    }

    /**
     * Construct an iterator over the given {@link StudentList} with a default retry quota of 3.
     *
     * @param list The API interface.
     */
    public StudentListIterator(StudentList list) {
        
        // Call the other constructor with the default retry quota
        this(list, 3);
    }

    @Override
    public boolean hasNext() {
    
        // Return true if there are more elements to iterate from the front or back
        return remainingStudents > 0 && (start <= end || currentPageIndex < list.getNumPages() - 1 || reversePageIndex >= 0);
        // Note remainingStudents > 0 is enough but the other conditions are added for clarity
    }

    @Override
    public Student next() {
        
        // Check if there are remaining elements to take
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate from the front");
        }

        // Load the page and initiliaze if not loaded yet
        if (currentPageIndex == -1) {
            currentPageIndex = 0;
            currentPage = loadPage(currentPageIndex);
            start = 0;
            end = currentPage.length - 1;
        }

        // If the current page is exhausted, load the next page
        if (start > end) {
            currentPage = loadPage(++currentPageIndex);
            start = 0;
            end = currentPage.length - 1; 
        }
        remainingStudents--;
        return currentPage[start++];
    }

    @Override
    public Student reverseNext() {
        
        // If there are no more elements to take, throw an exception
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate from the back");
        }

        // Load the page and initialize if not loaded yet
        if (reversePageIndex == -1) {
            currentPage = loadPage(reversePageNumber);
            reversePageIndex = currentPage.length - 1;
        }

        Student nextStudent = currentPage[reversePageIndex--];

        // If the current page is exhausted, load the previous page
        if (reversePageIndex < 0 && reversePageNumber >= 0) {
            currentPage = loadPage(--reversePageNumber);
            reversePageIndex = currentPage.length - 1;
        } 
        remainingStudents--;
        return nextStudent;
    }

    /**
     * Load a page of students from the API, with retries.
     *
     * @param pageIndex The index of the page to load.
     * @return An array of students in the specified page.
     * @throws ApiUnreachableException If the API is unreachable after the maximum number of retries.
     */
    private Student[] loadPage(int pageIndex) {
        int attempts = 1;
        while (attempts <= maxRetries) {
            try {
                return list.getPage(pageIndex);
            } catch (QueryTimedOutException e) {
                attempts++;
            }
        }
        throw new ApiUnreachableException(); // If the API is unreachable
    }
}
