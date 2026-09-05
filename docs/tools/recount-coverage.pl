use strict; use warnings; use utf8;
binmode STDOUT, ':encoding(UTF-8)';
my $dir = 'docs/specifications';
opendir my $d, $dir or die; my @f = sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $d; closedir $d;
my ($tc,$tp,$tn)=(0,0,0);
for my $n (@f) {
  my $p = "$dir/$n";
  open my $fh,'<:encoding(UTF-8)',$p or die; my @l = map { my $x=$_; $x=~s/\r?\n$//; $x } <$fh>; close $fh;
  my ($c,$pa,$nc)=(0,0,0); my $in=0;
  for my $i (0..$#l) {
    $in=1 if $l[$i] =~ /^## 8\. Traceability Matrix/;
    next unless $in;
    next unless $l[$i] =~ /^\|\s*[A-Z]{2}-AC\d+\s*\|/;
    my $row = $l[$i];
    if    ($row =~ /\|\s*\*\*Not covered/) { $nc++ }
    elsif ($row =~ /\|\s*\*\*Partial/)     { $pa++ }
    elsif ($row =~ /\|\s*Covered/)         { $c++  }
    else { print "  ?? unparsed row in $n: $row\n" }
  }
  my $new = "**Coverage:** $c Covered, $pa Partial, $nc Not covered.";
  my $changed = 0;
  for my $i (0..$#l) {
    if ($l[$i] =~ /^\*\*Coverage:\*\*/) { $changed = 1 if $l[$i] ne $new; $l[$i] = $new; }
  }
  if ($changed) {
    open my $o,'>:encoding(UTF-8)',$p or die; print $o join("\n",@l),"\n"; close $o;
    printf "%-42s %s\n", $n, $new;
  }
  $tc+=$c; $tp+=$pa; $tn+=$nc;
}
print "\nTOTAL: $tc Covered, $tp Partial, $tn Not covered\n";
