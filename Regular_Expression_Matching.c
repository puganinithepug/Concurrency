#include <stdio.h> 
#include <stdlib.h>
#include <string.h>
#include <omp.h>
#include <time.h>
#include <stdbool.h>

#define NUM_STATES 3
#define INITIAL_STATE 0
#define OK_STATE 0

int dfa_table[NUM_STATES][10] = {
    {0, 1, 2, 1, 1, 2, 2, 1, 2, 0},  //for state 0
    {1, 2, 0, 1, 2, 0, 0, 0, 2, 1},  //for state 1
    {1, 0, 1, 0, 2, 1, 1, 0, 2, 1}   //for state 2
};

// dfa processing logic for segment
int process_segment(const char *segment, int seg_length, int start_state) {
    int state = start_state;
    for (int i = 0; i < seg_length; i++) {
        state = dfa_table[state][segment[i] - '0'];  //fast lookup
    }
    return state;
}

//random string of base-10 digits length n
char *generate_random_digit_string(int n) {
    char *str = malloc((n + 1) * sizeof(char));
    if (!str) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }
    for (int i = 0; i < n; i++) {
        str[i] = '0' + (rand() % 10);
    }
    str[n] = '\0';
    return str;
}

int main(int argc, char *argv[]) {
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <t> <n>\n", argv[0]);
        exit(EXIT_FAILURE);
    }
//t is the number of optimistic threads.
// number of segment is m = t + 1.
// t == 0 is sequential
    int t = atoi(argv[1]);
    int n = atoi(argv[2]);

    if (t < 0 || n <= t) {
        fprintf(stderr, "Invalid input parameters. Ensure t >= 0 and n > t.\n");
        exit(EXIT_FAILURE);
    }

    //seed
    srand((unsigned int)time(NULL));

    //random input string
    char *input_string = generate_random_digit_string(n);

    //divide string into  m = t + 1 segments
    int m = t + 1;
    int seg_length = n / m;
    int remainder = n % m;  //remainder across m segments

    //arrays for segment pointers and segment lengths
    char **segments = malloc(m * sizeof(char *));
    int *seg_lengths = malloc(m * sizeof(int));
    if (!segments || !seg_lengths) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }
    int index = 0;
    for (int i = 0; i < m; i++) {
        int current_seg_length = seg_length + (i < remainder ? 1 : 0);
        segments[i] = input_string + index;
        seg_lengths[i] = current_seg_length;
        index += current_seg_length;
    }

    // start timing the matching
    double start_time = omp_get_wtime();

    // Process the first segment deterministically (starting from INITIAL_STATE).
    int current_state = process_segment(segments[0], seg_lengths[0], INITIAL_STATE);

    //mapping array for segments 1 to m-1
    // mapping[i][s] to store ending state
    int **mapping = NULL;
    if (m > 1) {
        mapping = malloc((m - 1) * sizeof(int *));
        if (!mapping) {
            perror("malloc");
            exit(EXIT_FAILURE);
        }
        for (int i = 0; i < m - 1; i++) {
            mapping[i] = malloc(NUM_STATES * sizeof(int));
            if (!mapping[i]) {
                perror("malloc");
                exit(EXIT_FAILURE);
            }
        }
    }

    //segments 1 to m-1 parallel
    //segment is processed for every possible starting state
    if (m > 1) {
        // using t threads for speculative processing
        #pragma omp parallel for num_threads(t) schedule(dynamic) \
            shared(mapping, segments, seg_lengths, m)
        for (int seg = 1; seg < m; seg++) {
            for (int s = 0; s < NUM_STATES; s++) {
                mapping[seg - 1][s] = process_segment(segments[seg], seg_lengths[seg], s);
            }
        }
    }

    // sequentially combine speculative mappings to determine final state.
    for (int seg = 1; seg < m; seg++) {
        current_state = mapping[seg - 1][current_state];
    }

    double end_time = omp_get_wtime();
    double elapsed_ms = (end_time - start_time) * 1000.0;

    //input string.
    // boolean" depending whether final state is OK_STATE.
    // time in milliseconds
    //printf("%s\n", input_string);
    printf("%s\n", (current_state == OK_STATE) ? "true" : "false");
    printf("%.3f\n", elapsed_ms);

    // free allocated memory.
    if (m > 1) {
        for (int i = 0; i < m - 1; i++) {
            free(mapping[i]);
        }
        free(mapping);
    }
    free(segments);
    free(seg_lengths);
    free(input_string);

    return 0;
}
