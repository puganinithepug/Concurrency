#include <stdio.h>
#include <stdlib.h>
#include <omp.h>
#include <time.h>

//print a matrix
void print_matrix(int *matrix, int n) {
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            printf("%d ", matrix[i * n + j]);
        }
        printf("\n");
    }
}

//print arrays
void print_array(const char *label, int *array, int size) {
    printf("%s: ", label);
    for (int i = 0; i < size; i++) {
        printf("%d ", array[i]);
    }
    printf("\n");
}

int main(int argc, char *argv[]) {
    if (argc != 4) {
        fprintf(stderr, "Usage: %s n p s\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    int n = atoi(argv[1]);
    double p = atof(argv[2]);
    unsigned int seed = (unsigned int)atoi(argv[3]);

    if (n <= 3 || p < 0.0 || p > 1.0) {
        fprintf(stderr, "Invalid input parameters.\n");
        exit(EXIT_FAILURE);
    }

    //matrix as a 1D array
    int *matrix = malloc(n * n * sizeof(int));
    if (!matrix) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }

    double start_time, end_time;
    
    //matrix creation parallelized
    start_time = omp_get_wtime();

    #pragma omp parallel shared(matrix, n, p, seed)
    {
        unsigned int thread_seed = seed + omp_get_thread_num();
        #pragma omp for schedule(static)
        for (int i = 0; i < n * n; i++) {
            double r = (double)rand_r(&thread_seed) / (RAND_MAX + 1.0);
            matrix[i] = (r < p) ? 0 : 1; // thread writes to unique index
        }
    }

    end_time = omp_get_wtime();
    double creation_time = end_time - start_time;

    // print generated matrix
    printf("Generated Matrix (%d x %d):\n", n, n);
    print_matrix(matrix, n);
    printf("\n");

    //matrix to CSR format
    double csr_start = omp_get_wtime();

    int *row_counts = malloc(n * sizeof(int));
    if (!row_counts) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }

    //count non-zeros per row, parallelized
    #pragma omp parallel for shared(matrix, row_counts, n) schedule(static)
    for (int i = 0; i < n; i++) {
        int count = 0;  //variable is local to each iteration
        for (int j = 0; j < n; j++) {
            if (matrix[i * n + j] != 0)
                count++;
        }
        row_counts[i] = count;
    }

    //serial prefix sum to build rowptr
    int total_nonzeros = 0;
    int *rowptr = malloc((n + 1) * sizeof(int));
    if (!rowptr) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }
    rowptr[0] = 0;
    for (int i = 0; i < n; i++) {
        total_nonzeros += row_counts[i];
        rowptr[i + 1] = total_nonzeros;
    }

    //allocate CSR arrays
    int *cols = malloc(total_nonzeros * sizeof(int));
    int *values = malloc(total_nonzeros * sizeof(int));
    if (!cols || !values) {
        perror("malloc");
        exit(EXIT_FAILURE);
    }

    //parallelized CSR conversion
    #pragma omp parallel for shared(matrix, rowptr, cols, values, n) schedule(static)
    for (int i = 0; i < n; i++) {
        int start = rowptr[i];  //start is local to this iteration
        for (int j = 0; j < n; j++) {
            if (matrix[i * n + j] != 0) {
                cols[start] = j;
                values[start] = 1;  //matrix contains only 0s and 1s
                start++;
            }
        }
    }

    double csr_end = omp_get_wtime();
    double csr_time = csr_end - csr_start;

    // print CSR representation
    printf("CSR Representation:\n");
    print_array("rowptr", rowptr, n + 1);
    print_array("cols", cols, total_nonzeros);
    print_array("values", values, total_nonzeros);

    // print performance timing
    printf("\nMatrix creation time: %.6f seconds\n", creation_time);
    printf("CSR conversion time: %.6f seconds\n", csr_time);

    // Free allocated memory
    free(matrix);
    free(row_counts);
    free(rowptr);
    free(cols);
    free(values);

    return 0;
}
