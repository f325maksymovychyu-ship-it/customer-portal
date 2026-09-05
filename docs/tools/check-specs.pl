#!/usr/bin/perl
# Verifies each spec in docs/specifications against its backlog source:
#   1. the AC set matches exactly (no missing, no invented)
#   2. every AC's Gherkin text is reproduced verbatim
#   3. every AC is referenced somewhere outside section 2 (no uncovered AC)
#   4. no [XX-ACn] tag references an AC that does not exist (no dangling tag)
#   5. every AC appears exactly once in the traceability matrix
# Run from the repository root:  perl docs/tools/check-specs.pl
use strict;
use warnings;
use utf8;
binmode STDOUT, ':encoding(UTF-8)';

my $BACKLOG = 'docs/backlog';
my $SPECS   = 'docs/specifications';

# read a file, return { AC_ID => gherkin_text }, in order
sub acs {
    my ($path) = @_;
    open my $fh, '<:encoding(UTF-8)', $path or die "$path: $!";
    my @l = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;
    my (%out, @order);
    for (my $i = 0; $i < @l; $i++) {
        next unless $l[$i] =~ /^\*\*([A-Z]{2}-AC\d+)\s+—/;
        my $id = $1;
        my $j = $i + 1;
        $j++ while $j < @l && $l[$j] !~ /^```gherkin/;
        next unless $j < @l;
        my @body;
        $j++;
        push @body, $l[$j++] while $j < @l && $l[$j] !~ /^```/;
        $out{$id} = join("\n", @body);
        push @order, $id;
        $i = $j;
    }
    return (\%out, \@order);
}

sub section_of {
    my ($path, $heading_re) = @_;
    open my $fh, '<:encoding(UTF-8)', $path or die "$path: $!";
    my @l = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;
    my (@body, $in);
    for my $line (@l) {
        if ($line =~ /^##\s+(.*)$/) { $in = ($1 =~ $heading_re) ? 1 : 0; next; }
        push @body, $line if $in;
    }
    return join("\n", @body);
}

sub slurp {
    my ($p) = @_;
    open my $fh, '<:encoding(UTF-8)', $p or die "$p: $!";
    local $/; my $s = <$fh>; close $fh; return $s;
}

# every AC defined anywhere in the backlog, so cross-story references can be validated
our %ALL_ACS;
{
    opendir my $bd, $BACKLOG or die "$BACKLOG: $!";
    for my $f (grep { /^US-\d+\.\d+-.*\.md$/ } readdir $bd) {
        my ($a) = acs("$BACKLOG/$f");
        $ALL_ACS{$_} = $f for keys %$a;
    }
    closedir $bd;
}

opendir my $dh, $SPECS or die "$SPECS: $!";
my @specs = sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $dh;
closedir $dh;
@specs = sort {
    my ($ae,$as) = $a =~ /^US-(\d+)\.(\d+)/; my ($be,$bs) = $b =~ /^US-(\d+)\.(\d+)/;
    $ae <=> $be || $as <=> $bs;
} @specs;

my ($files, $problems) = (0, 0);
for my $name (@specs) {
    my $spec = "$SPECS/$name";
    my $back = "$BACKLOG/$name";
    $files++;
    my @issues;

    unless (-f $back) { print "FAIL $name\n  no backlog source\n"; $problems++; next; }

    my ($sa, $so) = acs($spec);
    my ($ba, $bo) = acs($back);

    # 1. set equality
    my %only_spec = map { $_ => 1 } grep { !exists $ba->{$_} } keys %$sa;
    my %only_back = map { $_ => 1 } grep { !exists $sa->{$_} } keys %$ba;
    push @issues, "invented AC: " . join(', ', sort keys %only_spec) if %only_spec;
    push @issues, "missing AC: "  . join(', ', sort keys %only_back) if %only_back;

    # 2. verbatim text
    for my $id (@$bo) {
        next unless exists $sa->{$id};
        push @issues, "$id text differs from source" if $sa->{$id} ne $ba->{$id};
    }

    # 3/4. coverage and dangling tags, outside section 2
    my $body = slurp($spec);
    my $ac_section = section_of($spec, qr/^2\.\s+Acceptance Criteria/);
    my $outside = $body;
    $outside =~ s/\Q$ac_section\E//;
    for my $id (@$bo) {
        push @issues, "$id never referenced outside section 2" unless $outside =~ /\Q$id\E/;
    }
    # A tag carrying this story's own prefix must name one of its ACs. A tag with another
    # story's prefix is a cross-reference and is checked against that story instead.
    my ($own_prefix) = ($bo->[0] // '') =~ /^([A-Z]{2})-/;
    my %known = map { $_ => 1 } @$bo;
    my %seen;
    while ($body =~ /([A-Z]{2})-AC(\d+)/g) {
        my ($pfx, $n) = ($1, $2);
        my $t = "$pfx-AC$n";
        next if $seen{$t}++;
        if (defined $own_prefix && $pfx eq $own_prefix) {
            push @issues, "dangling tag $t" unless $known{$t};
        } else {
            push @issues, "cross-reference $t matches no AC in any story"
                unless exists $ALL_ACS{$t};
        }
    }

    # 5. matrix rows — one row per AC, identified by the row's first cell
    my $matrix = section_of($spec, qr/^8\.\s+Traceability Matrix/);
    my %rows;
    for my $line (split /\n/, $matrix) {
        next unless $line =~ /^\|\s*([A-Z]{2}-AC\d+)\s*\|/;
        $rows{$1}++;
    }
    for my $id (@$bo) {
        my $n = $rows{$id} // 0;
        push @issues, "$id has $n matrix rows (expected 1)" if $n != 1;
    }
    for my $id (keys %rows) {
        push @issues, "matrix row for unknown $id" unless $known{$id};
    }

    if (@issues) {
        $problems++;
        print "FAIL $name\n";
        print "  - $_\n" for @issues;
    } else {
        printf "OK   %-42s %2d ACs verbatim, covered, matrixed\n", $name, scalar @$bo;
    }
}

printf "\n%d spec(s) checked, %d with problems\n", $files, $problems;
exit($problems ? 1 : 0);
