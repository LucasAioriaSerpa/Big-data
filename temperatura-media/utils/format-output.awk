function split_pair(token, output, position,    count, pieces, i) {
    count = split(token, pieces, "=")
    if (count == 1) {
        output["name"] = correlation_names[position]
        output["value"] = token
    } else {
        output["value"] = pieces[count]
        output["name"] = pieces[1]
        for (i = 2; i < count; i++) {
            output["name"] = output["name"] "=" pieces[i]
        }
    }
}

function print_pairs(title, values, separator,    count, i, parts, left, right) {
    count = split(values, parts, separator)
    printf "\n%s\n", title
    for (i = 1; i <= count; i += 2) {
        split_pair(parts[i], left, i)
        if (i + 1 <= count) {
            split_pair(parts[i + 1], right, i + 1)
            printf "  %-18s %-14s | %-18s %-14s\n",
                left["name"], left["value"], right["name"], right["value"]
        } else {
            printf "  %-18s %-14s\n", left["name"], left["value"]
        }
    }
}

function print_centroid(title, values,    count, parts, i) {
    count = split(values, parts, ",")
    printf "\n%s\n", title
    for (i = 1; i <= count; i += 2) {
        if (i + 1 <= count) {
            printf "  %-18s %-14s | %-18s %-14s\n",
                centroid_names[i], parts[i], centroid_names[i + 1], parts[i + 1]
        } else {
            printf "  %-18s %-14s\n", centroid_names[i], parts[i]
        }
    }
}

{
    if (header_done) {
        printf "\n%s\n", "----------------------------------------"
    }
    header_done = 1
    k = $1
    sub(/^k=/, "", k)
    printf "\nK: %-3s | Cluster: %-3s | Registros: %s\n", k, $2, $3
    if ($3 == 0) {
        printf "Status: sem registros; correlações não calculadas.\n"
        next
    }
    print_centroid("Centróide normalizado:", $4)
    print_pairs("Correlações (temperatura x variável):", $5, ";")
}

BEGIN {
    centroid_names[1] = "X"
    centroid_names[2] = "Y"
    centroid_names[3] = "FFMC"
    centroid_names[4] = "DMC"
    centroid_names[5] = "DC"
    centroid_names[6] = "ISI"
    centroid_names[7] = "temp"
    centroid_names[8] = "RH"
    centroid_names[9] = "wind"
    centroid_names[10] = "rain"

    correlation_names[1] = "X"
    correlation_names[2] = "Y"
    correlation_names[3] = "FFMC"
    correlation_names[4] = "DMC"
    correlation_names[5] = "DC"
    correlation_names[6] = "ISI"
    correlation_names[7] = "RH"
    correlation_names[8] = "wind"
    correlation_names[9] = "rain"
    correlation_names[10] = "area"
    correlation_names[11] = "month=jan"
    correlation_names[12] = "month=feb"
    correlation_names[13] = "month=mar"
    correlation_names[14] = "month=apr"
    correlation_names[15] = "month=may"
    correlation_names[16] = "month=jun"
    correlation_names[17] = "month=jul"
    correlation_names[18] = "month=aug"
    correlation_names[19] = "month=sep"
    correlation_names[20] = "month=oct"
    correlation_names[21] = "month=nov"
    correlation_names[22] = "month=dec"
    correlation_names[23] = "day=mon"
    correlation_names[24] = "day=tue"
    correlation_names[25] = "day=wed"
    correlation_names[26] = "day=thu"
    correlation_names[27] = "day=fri"
    correlation_names[28] = "day=sat"
    correlation_names[29] = "day=sun"
}
